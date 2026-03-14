// js/rag.js
async function askQuestion() {
    const input = document.getElementById('questionInput');
    const q = input.value.trim();
    if (!q || !pdfLoaded) return;
    input.value = '';
    addMessage('user', q);
    const loadId = addThinking();

    try {
        const res = await fetch('/api/rag/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question: q })
        });
        const api = await res.json();
        removeMsg(loadId);
        addMessage('bot', api.success ? api.data : '❌ ' + api.message);
    } catch(e) {
        removeMsg(loadId);
        addMessage('bot', '❌ Network error');
    }
}

let selectedFile = null, pdfLoaded = false, messageCount = 0;

window.onload = async () => {
  try {
    const res = await fetch('/api/rag/status');
    const data = await res.json();
    if (data.hasDocument) activatePdf(data.documentName || 'Loaded document');
    if (data.hasHistory) updateMemoryUI(true);
  } catch(e) {}
};

function handleFileSelect(input) {
  selectedFile = input.files[0];
  if (selectedFile) { document.getElementById('fileName').textContent = selectedFile.name; document.getElementById('fileInfo').style.display = 'block'; }
}

async function uploadPdf() {
  if (!selectedFile) return;
  const btn = document.getElementById('uploadBtn');
  btn.innerHTML = '<span class="spinner"></span> Processing...'; btn.disabled = true;
  const formData = new FormData(); formData.append('file', selectedFile);
  try {
    const res = await fetch('/api/rag/upload', { method:'POST', body:formData });
    const data = await res.json();
    if (data.success) {
      document.getElementById('uploadStatus').innerHTML = `<div class="status-msg status-success"><i class="fas fa-check-circle me-1"></i>${data.message}</div>`;
      activatePdf(selectedFile.name);
      addMessage('bot', `✅ **${selectedFile.name}** loaded successfully!\n\nYou can now:\n• Ask any question about the content\n• Click "Summarize PDF" for an overview\n• Click "Generate Quiz" to test knowledge`);
    } else {
      document.getElementById('uploadStatus').innerHTML = `<div class="status-msg status-error">${data.message}</div>`;
      btn.innerHTML = '<i class="fas fa-upload me-2"></i>Process PDF'; btn.disabled = false;
    }
  } catch(e) {
    document.getElementById('uploadStatus').innerHTML = '<div class="status-msg status-error">Upload failed. Try again.</div>';
    btn.innerHTML = '<i class="fas fa-upload me-2"></i>Process PDF'; btn.disabled = false;
  }
}

function activatePdf(name) {
  pdfLoaded = true;
  document.getElementById('uploadZoneWrap').style.display = 'none';
  document.getElementById('loadedActions').style.display = 'block';
  document.getElementById('emptyState').style.display = 'none';
  document.getElementById('chatStatus').textContent = name || 'PDF Ready';
  document.getElementById('statusDot').style.background = 'var(--green)';
  document.getElementById('questionInput').disabled = false;
  document.getElementById('askBtn').disabled = false;
}

async function askQuestion() {
  const input = document.getElementById('questionInput');
  const q = input.value.trim();
  if (!q || !pdfLoaded) return;
  input.value = '';
  addMessage('user', q);
  const loadId = addThinking();

  try {
    const res = await fetch('/api/rag/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: q })
    });
    const api = await res.json();
    removeMsg(loadId);
    addMessage('bot', api.success ? api.data : '❌ ' + api.message);
  } catch(e) {
    removeMsg(loadId);
    addMessage('bot', '❌ Network error');
  }
}

function updateMemoryUI(hasMessages) {
  const btn = document.getElementById('clearHistoryBtn');
  const count = document.getElementById('msgCount');
  if (hasMessages) {
    btn.style.display = 'block';
    count.textContent = `🗂️ ${messageCount > 0 ? messageCount + ' new messages' : 'History loaded from DB'}`;
    count.style.color = 'var(--green)';
  } else {
    btn.style.display = 'none';
    count.textContent = 'No messages yet';
    count.style.color = 'var(--text-muted)';
  }
}

async function clearHistory() {
  if (!confirm('Clear all conversation memory for this session?')) return;
  await fetch('/api/rag/history', { method: 'DELETE' });
  messageCount = 0;
  updateMemoryUI(false);
  addMessage('bot', '🧹 Conversation memory cleared! Starting fresh.');
}

async function summarizePdf() {
  addMessage('user', 'Please summarize this PDF document.');
  const loadId = addThinking();
  try {
    const res = await fetch('/api/rag/summarize', { method:'POST' });
    const data = await res.json();
    removeMsg(loadId);
    addMessage('bot', data.success ? data.summary : '❌ ' + data.message);
  } catch(e) { removeMsg(loadId); addMessage('bot', '❌ Error generating summary.'); }
}

async function generateQuiz() {
  addMessage('user', 'Generate a quiz from this PDF.');
  const loadId = addThinking();
  try {
    const res = await fetch('/api/rag/quiz', { method:'POST' });
    const data = await res.json();
    removeMsg(loadId);
    if (data.success) {
      sessionStorage.setItem('quizData', data.quiz);
      addMessage('bot', '✅ Quiz generated! Redirecting to quiz page...');
      setTimeout(() => window.location.href = 'quiz.html', 1500);
    } else { addMessage('bot', '❌ ' + data.message); }
  } catch(e) { removeMsg(loadId); addMessage('bot', '❌ Error generating quiz.'); }
}

async function clearPdf() {
  await fetch('/api/rag/clear', { method:'DELETE' });
  pdfLoaded = false; messageCount = 0; updateMemoryUI(false);
  document.getElementById('uploadZoneWrap').style.display = 'block';
  document.getElementById('loadedActions').style.display = 'none';
  document.getElementById('fileInfo').style.display = 'none';
  document.getElementById('uploadStatus').innerHTML = '';
  document.getElementById('uploadBtn').innerHTML = '<i class="fas fa-upload me-2"></i>Process PDF';
  document.getElementById('uploadBtn').disabled = false;
  document.getElementById('pdfInput').value = '';
  document.getElementById('statusDot').style.background = 'var(--border)';
  document.getElementById('chatStatus').textContent = 'Upload a PDF to start chatting';
  document.getElementById('questionInput').disabled = true;
  document.getElementById('askBtn').disabled = true;
  clearMessages();
  document.getElementById('emptyState').style.display = 'block';
  selectedFile = null;
}

function clearMessages() {
  const box = document.getElementById('chatMessages');
  box.innerHTML = '';
  if (!pdfLoaded) box.innerHTML = `<div style="text-align:center;margin:auto;color:var(--text-muted);padding:40px 20px;" id="emptyState"><div style="font-size:56px;margin-bottom:16px;">📄</div><div style="font-weight:800;color:var(--text-secondary);margin-bottom:8px;font-family:'Nunito',sans-serif;font-size:1.1rem;">No PDF Loaded Yet</div><div style="font-size:0.85rem;">Upload a PDF from the sidebar to begin</div></div>`;
}

function addMessage(type, text) {
  const box = document.getElementById('chatMessages');
  const div = document.createElement('div');
  div.className = type === 'user' ? 'msg msg-user' : 'msg msg-bot';
  div.textContent = text;
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
}

function addThinking() {
  const box = document.getElementById('chatMessages');
  const id = 'think-' + Date.now();
  box.innerHTML += `<div class="msg-thinking" id="${id}"><div class="dots"><span></span><span></span><span></span></div><span>Thinking...</span></div>`;
  box.scrollTop = box.scrollHeight;
  return id;
}
function removeMsg(id) { document.getElementById(id)?.remove(); }