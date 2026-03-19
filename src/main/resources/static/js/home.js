  // ── Navbar: user info load ──────────────────────────
  fetch('/api/auth/me')
    .then(r => r.json())
    .then(user => {
      if (user.authenticated) {
        document.getElementById('navAvatar').src = user.picture;
        document.getElementById('navName').textContent = user.name.split(' ')[0];
        document.getElementById('navUser').style.display = 'flex';
      }
    })
    .catch(() => {});

  // ── Navbar scroll effect ────────────────────────────
  window.addEventListener('scroll', () =>
    document.getElementById('navbar').classList.toggle('scrolled', window.scrollY > 10)
  );

  // ── Tabs ────────────────────────────────────────────
  const tabPills = document.querySelectorAll('.tab-pill');
  function switchTab(n) {
    tabPills.forEach((t, i) => {
      t.classList.toggle('active', i === n);
      document.getElementById('tab' + i).classList.toggle('active', i === n);
    });
  }

  // ── MCP nav ─────────────────────────────────────────
  function mcpNav(tool) {
    const links = { summarize:'summarize.html', translate:'translate.html', quiz:'quiz.html', ask:'#ask-section', search:'rag.html' };
    const msgs  = { search:'Opening PDF Chat...', summarize:'Opening Summarizer...', translate:'Opening Translator...', quiz:'Opening Quiz...', ask:'Scrolling to Ask...' };
    document.getElementById('mcpMsg').textContent = '→ ' + msgs[tool];
    setTimeout(() => {
      const t = links[tool]; if (!t) return;
      if (t.startsWith('#')) document.querySelector(t)?.scrollIntoView({ behavior: 'smooth' });
      else window.location.href = t;
      document.getElementById('mcpMsg').textContent = '';
    }, 700);
  }

  // ── Ask AI ──────────────────────────────────────────
  async function callAsk() {
  const input = document.getElementById('askInput').value.trim();
  if (!input) return;
  const btn = document.getElementById('askBtn');
  btn.innerHTML = '<span class="spinner"></span> Thinking...'; btn.disabled = true;

  try {
    const res = await fetch('/api/gemini/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: input })
    });
    const api = await res.json();
    
    if (api.success) {
      showOutput('askOutput', api.data);
      document.getElementById('memoryHint').style.display = 'flex';
    } else {
      showOutput('askOutput', '❌ ' + (api.message || 'Error'));
    }
  } catch (e) {
    showOutput('askOutput', '❌ Network error');
  }
  
  btn.innerHTML = '<i class="fas fa-paper-plane me-2"></i>Get Answer';
  btn.disabled = false;
}

  // ── Quick PDF upload ────────────────────────────────
  async function quickUpload() {
    const file = document.getElementById('ragFile').files[0];
    if (!file) return;
    document.getElementById('quickUploadStatus').textContent = '⏳ Processing...';
    const form = new FormData(); form.append('file', file);
    const res  = await fetch('/api/rag/upload', { method: 'POST', body: form });
    const data = await res.json();
    document.getElementById('quickUploadStatus').textContent = data.success ? '✅ ' + file.name : '❌ Failed';
  }

  // ── RAG ask ─────────────────────────────────────────
  async function callRagAsk() {
  const q = document.getElementById('ragInput').value.trim();
  if (!q) return;
  try {
    const res = await fetch('/api/rag/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: q })
    });
    const api = await res.json();
    showOutput('ragOutput', api.success ? api.data : '❌ ' + api.message);
  } catch (e) {
    showOutput('ragOutput', '❌ Error');
  }
}

  // ── Clear ask history ───────────────────────────────
  async function clearAskHistory() {
    await fetch('/api/gemini/history?feature=ask', { method: 'DELETE' });
    document.getElementById('memoryHint').style.display = 'none';
    document.getElementById('askOutput').classList.add('hidden');
  }

  // ── Utility ─────────────────────────────────────────
  function showOutput(id, text) {
    const el = document.getElementById(id);
    el.textContent = text;
    el.classList.remove('hidden');
  }
  //logout
  function doLogout() {
  fetch('/api/auth/logout', { method: 'POST' })
    .then(() => window.location.href = '/login.html')
    .catch(() => window.location.href = '/login.html');
}