// js/summarize.js
async function callSummarize() {
    const text = document.getElementById('sumInput').value.trim();
    if (!text) return;
    const btn = document.getElementById('sumBtn');
    btn.innerHTML = '<span class="spinner"></span> Summarizing...'; btn.disabled = true;

    try {
        const res = await fetch('/api/gemini/summarize', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: text })
        });
        const api = await res.json();
        const out = document.getElementById('sumOutput');
        out.textContent = api.success ? api.data : '❌ ' + (api.message || 'Error');
        out.classList.remove('hidden');
        document.getElementById('sumActions').style.display = 'flex';
    } catch(e) {
        document.getElementById('sumOutput').textContent = '❌ Failed';
    }
    btn.innerHTML = '<i class="fas fa-compress-alt me-2"></i>Summarize'; btn.disabled = false;
}


function clearAll() {
  document.getElementById('sumInput').value = '';
  document.getElementById('charCount').textContent = '0 chars';
  document.getElementById('sumOutput').classList.add('hidden');
  document.getElementById('sumActions').style.display = 'none';
}
function copyOutput() {
  const text = document.getElementById('sumOutput').textContent;
  navigator.clipboard.writeText(text).then(() => {
    const btn = event.target.closest('button');
    btn.innerHTML = '<i class="fas fa-check me-1"></i>Copied!';
    setTimeout(() => btn.innerHTML = '<i class="fas fa-copy me-1"></i>Copy', 2000);
  });
}