// js/rag.js
let selectedFile = null, pdfLoaded = false, messageCount = 0;
let isSmartMode = false;

window.onload = async () => {
  try {
    const res = await fetch('/api/rag/status');
    const data = await res.json();
    if (data.hasDocument) activatePdf(data.documentName || 'Loaded document');
    // Note: status response now uses hasDocument and documentName per RagController.java
  } catch(e) {}
};

function toggleSmartMode() {
    isSmartMode = !isSmartMode;
    const btn = document.getElementById('smartModeBtn');
    if (isSmartMode) {
        btn.classList.add('btn-primary');
        btn.classList.remove('btn-ghost');
        btn.innerHTML = '🤖 Smart Agent: ON';
        addMessage('bot', 'Librarian Agent Activated! I can now automatically summarize, create quizzes, or translate based on your questions.');
    } else {
        btn.classList.add('btn-ghost');
        btn.classList.remove('btn-primary');
        btn.innerHTML = '🤖 Smart Agent: OFF';
    }
}

async function askQuestion() {
  const input = document.getElementById('questionInput');
  const q = input.value.trim();
  if (!q || !pdfLoaded) return;
  input.value = '';
  addMessage('user', q);
  const loadId = addThinking();

  try {
    let url = isSmartMode ? '/api/cef/agent/route' : '/api/gemini/ask';
    let body = isSmartMode ? { intent: q } : { question: q };

    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const api = await res.json();
    removeMsg(loadId);
    
    // Agent response format is different { toolUsed, resultData }
    let msg = api.success ? (isSmartMode ? api.data.resultData : api.data) : '❌ ' + api.message;
    addMessage('bot', msg);
    
    if (isSmartMode && api.data.toolUsed === 'QUIZ') {
        sessionStorage.setItem('quizData', api.data.resultData);
        addMessage('bot', 'Redirecting to quiz page in 2 seconds...');
        setTimeout(() => window.location.href = 'quiz.html', 2000);
    }
  } catch(e) {
    removeMsg(loadId);
    addMessage('bot', '❌ Network error');
  }
}

function handleFileSelect(input) {
  selectedFile = input.files[0];
  if (selectedFile) {
    document.getElementById('fileName').textContent = selectedFile.name;
    document.getElementById('fileInfo').style.display = 'block';
  }
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
      addMessage('bot', `✅ **${selectedFile.name}** loaded successfully!`);
    } else {
      document.getElementById('uploadStatus').innerHTML = `<div class="status-msg status-error">${data.message}</div>`;
      btn.innerHTML = '<i class="fas fa-upload me-2"></i>Process PDF'; btn.disabled = false;
    }
  } catch(e) {
    document.getElementById('uploadStatus').innerHTML = '<div class="status-msg status-error">Upload failed.</div>';
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

async function summarizePdf() {
  addMessage('user', 'Please summarize this PDF document.');
  const loadId = addThinking();
  try {
    const res = await fetch('/api/gemini/summarize', { method:'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({}) });
    const data = await res.json();
    removeMsg(loadId);
    addMessage('bot', data.success ? data.data : '❌ ' + data.message);
  } catch(e) { removeMsg(loadId); addMessage('bot', '❌ Error generating summary.'); }
}

async function generateQuiz() {
  addMessage('user', 'Generate a quiz from this PDF.');
  const loadId = addThinking();
  try {
    const res = await fetch('/api/gemini/quiz', { method:'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({}) });
    const data = await res.json();
    removeMsg(loadId);
    if (data.success) {
      sessionStorage.setItem('quizData', data.data);
      addMessage('bot', '✅ Quiz generated! Redirecting...');
      setTimeout(() => window.location.href = 'quiz.html', 1500);
    } else { addMessage('bot', '❌ ' + data.message); }
  } catch(e) { removeMsg(loadId); addMessage('bot', '❌ Error generating quiz.'); }
}

async function clearPdf() {
  await fetch('/api/rag/clear', { method:'DELETE' });
  location.reload();
}

function addMessage(type, text) {
  const box = document.getElementById('chatMessages');
  const div = document.createElement('div');
  div.className = type === 'user' ? 'msg msg-user' : 'msg msg-bot';
  div.innerHTML = formatMarkdown(text);
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
}

function formatMarkdown(text) {
    if (!text) return "";
    return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
               .replace(/\*(.*?)\*/g, '<em>$1</em>')
               .replace(/\n/g, '<br>');
}

function addThinking() {
  const box = document.getElementById('chatMessages');
  const id = 'think-' + Date.now();
  const div = document.createElement('div');
  div.id = id;
  div.className = 'msg-thinking';
  div.innerHTML = `<div class="dots"><span></span><span></span><span></span></div><span>Thinking...</span>`;
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
  return id;
}

function removeMsg(id) { document.getElementById(id)?.remove(); }