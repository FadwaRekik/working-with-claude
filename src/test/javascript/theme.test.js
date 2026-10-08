const fs = require('fs');
const path = require('path');
const { loadApp } = require('./setup/loadApp');

const STATIC_DIR = path.resolve(__dirname, '../../main/resources/static');
const THEME_KEY = 'ops-dashboard-theme';

function theme(document) {
  return document.documentElement.getAttribute('data-theme');
}

beforeEach(() => {
  localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
  delete window.matchMedia;
});

describe('theme toggle (TODO-231)', () => {
  test('AC-1, AC-2: the header toggle flips data-theme, and colours live in CSS variables only', async () => {
    const { document } = await loadApp();
    const toggle = document.getElementById('theme-toggle');
    expect(document.getElementById('app-header').contains(toggle)).toBe(true);

    expect(theme(document)).toBe('dark');
    expect(toggle.textContent).toBe('Light theme');

    toggle.click();
    expect(theme(document)).toBe('light');
    expect(toggle.textContent).toBe('Dark theme');

    toggle.click();
    expect(theme(document)).toBe('dark');
    expect(toggle.getAttribute('aria-label')).toBe('Switch to light theme');

    const css = fs.readFileSync(path.join(STATIC_DIR, 'style.css'), 'utf8');
    expect(css).toMatch(/\[data-theme="light"\]\s*\{/);
    for (const selector of ['.chart-svg .bar', '.chart-svg .bar.warn', '.chart-svg .bar-label', '.chart-svg .bar-value']) {
      const escaped = selector.replace(/\./g, '\\.');
      expect(css).toMatch(new RegExp(escaped + '\\s*\\{\\s*fill:\\s*var\\(--'));
    }
    const js = fs.readFileSync(path.join(STATIC_DIR, 'app.js'), 'utf8');
    expect(js).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
  });

  test('AC-3: the choice is stored in localStorage and restored on load', async () => {
    const first = await loadApp();
    first.document.getElementById('theme-toggle').click();
    expect(localStorage.getItem(THEME_KEY)).toBe('light');

    // The inline <head> script applies the stored theme before first paint, before app.js runs.
    document.documentElement.removeAttribute('data-theme');
    const html = fs.readFileSync(path.join(STATIC_DIR, 'index.html'), 'utf8');
    const headScript = html.match(/<head>[\s\S]*?<script>([\s\S]*?)<\/script>[\s\S]*?<\/head>/)[1];
    new Function(headScript)();
    expect(theme(document)).toBe('light');

    const second = await loadApp();
    expect(theme(second.document)).toBe('light');
    expect(second.document.getElementById('theme-toggle').textContent).toBe('Dark theme');
  });

  test('AC-4: dark by default, ignoring the OS setting and invalid stored values', async () => {
    window.matchMedia = jest.fn(() => ({ matches: true, media: '(prefers-color-scheme: light)' }));
    const { document } = await loadApp();
    expect(theme(document)).toBe('dark');
    expect(window.matchMedia).not.toHaveBeenCalled();

    localStorage.setItem(THEME_KEY, 'purple');
    const again = await loadApp();
    expect(theme(again.document)).toBe('dark');
  });
});
