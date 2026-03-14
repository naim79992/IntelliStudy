// js/app.js — Shared utilities for all pages
function toggleNav() {
    const nav = document.getElementById('navLinks');
    if (nav) nav.classList.toggle('open');
}

function doLogout() {
    fetch('/api/auth/logout', { method: 'POST' })
        .then(() => window.location.href = '/login.html')
        .catch(() => window.location.href = '/login.html');
}

// Common output helper
function showOutput(id, text) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = text;
        el.classList.remove('hidden');
    }
}

// MCP navigation helper
function mcpNav(tool) {
    const links = {
        summarize: 'summarize.html',
        translate: 'translate.html',
        quiz: 'quiz.html',
        ask: '#ask-section',
        search: 'rag.html'
    };
    const msgs = {
        search: 'Opening PDF Chat...',
        summarize: 'Opening Summarizer...',
        translate: 'Opening Translator...',
        quiz: 'Opening Quiz...',
        ask: 'Scrolling to Ask...'
    };
    const msgEl = document.getElementById('mcpMsg');
    if (msgEl) msgEl.textContent = '→ ' + (msgs[tool] || '');
    
    setTimeout(() => {
        const target = links[tool];
        if (!target) return;
        if (target.startsWith('#')) {
            document.querySelector(target)?.scrollIntoView({ behavior: 'smooth' });
        } else {
            window.location.href = target;
        }
        if (msgEl) msgEl.textContent = '';
    }, 700);
}