// js/translate.js
async function callTranslate() {
    const text = document.getElementById('translateInput').value.trim();
    if (!text) return;
    const btn = document.getElementById('transBtn');
    btn.innerHTML = '<span class="spinner"></span> Translating...'; btn.disabled = true;

    try {
        const res = await fetch('/api/gemini/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: text })
        });
        const api = await res.json();
        const out = document.getElementById('translateOutput');
        out.textContent = api.success ? api.data : '❌ ' + (api.message || 'Error');
        out.classList.add('has-text');
        document.getElementById('copyBtn').style.display = 'inline-flex';
    } catch(e) {
        document.getElementById('translateOutput').textContent = '❌ Failed';
    }
    btn.innerHTML = '<i class="fas fa-language me-2"></i>Translate Now'; btn.disabled = false;
}




function clearAll() {
  document.getElementById('translateInput').value = '';
  const out = document.getElementById('translateOutput');
  out.textContent = 'Translation will appear here...'; out.classList.remove('has-text');
  document.getElementById('copyBtn').style.display = 'none';
}
function copyTranslation() {
  navigator.clipboard.writeText(document.getElementById('translateOutput').textContent).then(() => {
    const btn = document.getElementById('copyBtn');
    btn.innerHTML = '<i class="fas fa-check me-1"></i>Copied!';
    setTimeout(() => btn.innerHTML = '<i class="fas fa-copy me-1"></i>Copy', 2000);
  });
}