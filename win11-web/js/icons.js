/* Fluent-inspired icon sprite, drawn from scratch for this demo. */
(function () {
  const sprite = `
  <svg id="icon-sprite" xmlns="http://www.w3.org/2000/svg" style="position:absolute;width:0;height:0;overflow:hidden" aria-hidden="true">
    <defs>
      <linearGradient id="ig-win" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#43c6ff"/><stop offset="1" stop-color="#0369c9"/>
      </linearGradient>
      <linearGradient id="ig-folder-back" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#ffd75e"/><stop offset="1" stop-color="#f0a92c"/>
      </linearGradient>
      <linearGradient id="ig-folder-front" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#ffe58f"/><stop offset="1" stop-color="#ffc63d"/>
      </linearGradient>
      <linearGradient id="ig-note" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#4f9cf7"/><stop offset="1" stop-color="#175bb5"/>
      </linearGradient>
      <linearGradient id="ig-calc" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#66b3ff"/><stop offset="1" stop-color="#1a5eb8"/>
      </linearGradient>
      <linearGradient id="ig-term" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#263445"/><stop offset="1" stop-color="#0a1019"/>
      </linearGradient>
      <linearGradient id="ig-gear" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#c3d9f2"/><stop offset=".55" stop-color="#7aa7d8"/><stop offset="1" stop-color="#4779ad"/>
      </linearGradient>
      <linearGradient id="ig-sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#5fc8ff"/><stop offset="1" stop-color="#1a6fd4"/>
      </linearGradient>
      <linearGradient id="ig-metal" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#eef4fb"/><stop offset="1" stop-color="#9ab0c8"/>
      </linearGradient>
      <linearGradient id="ig-screen" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#38b6ff"/><stop offset="1" stop-color="#0b4f9e"/>
      </linearGradient>
      <linearGradient id="ig-paper" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#ffffff"/><stop offset="1" stop-color="#e8eef6"/>
      </linearGradient>
      <linearGradient id="ig-bin" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#dbeafe" stop-opacity=".95"/><stop offset="1" stop-color="#8fb8dd" stop-opacity=".78"/>
      </linearGradient>
      <linearGradient id="ig-green" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#6ee7b7"/><stop offset="1" stop-color="#0f9d76"/>
      </linearGradient>
      <linearGradient id="ig-weather" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#63c7ff"/><stop offset="1" stop-color="#1666d6"/>
      </linearGradient>
      <linearGradient id="ig-photo-sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#7dd3fc"/><stop offset="1" stop-color="#e0f2fe"/>
      </linearGradient>
    </defs>

    <!-- Windows logo -->
    <symbol id="app-win" viewBox="0 0 48 48">
      <rect x="7" y="7" width="16" height="16" rx="1.5" fill="url(#ig-win)"/>
      <rect x="25" y="7" width="16" height="16" rx="1.5" fill="url(#ig-win)" opacity=".92"/>
      <rect x="7" y="25" width="16" height="16" rx="1.5" fill="url(#ig-win)" opacity=".92"/>
      <rect x="25" y="25" width="16" height="16" rx="1.5" fill="url(#ig-win)" opacity=".84"/>
    </symbol>

    <!-- File Explorer -->
    <symbol id="app-explorer" viewBox="0 0 48 48">
      <path d="M5 15.2C5 12.9 6.9 11 9.2 11h10.1l3.6 3.8h16.1c2.3 0 4.2 1.9 4.2 4.2v17.6c0 2.3-1.9 4.2-4.2 4.2H9.2A4.2 4.2 0 0 1 5 36.6V15.2z" fill="url(#ig-folder-back)"/>
      <rect x="17" y="8" width="15" height="15" rx="2" fill="#fff" opacity=".96"/>
      <path d="M18.5 12h12M18.5 15.4h8.5" stroke="#9fb6cf" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M5 25c0-2 1.6-3.6 3.6-3.6h31.8c2 0 3.6 1.6 3.6 3.6v11.4A3.6 3.6 0 0 1 40.4 40H8.6A3.6 3.6 0 0 1 5 36.4V25z" fill="url(#ig-folder-front)"/>
      <path d="M5 27.2c0-1 .8-1.8 1.8-1.8h34.4c1 0 1.8.8 1.8 1.8" stroke="#fff2b8" stroke-width="1.2" opacity=".8"/>
    </symbol>

    <!-- Notepad -->
    <symbol id="app-notepad" viewBox="0 0 48 48">
      <rect x="9" y="7" width="30" height="36" rx="4.5" fill="url(#ig-note)"/>
      <rect x="13.5" y="13" width="21" height="25.5" rx="2.2" fill="#f7fbff"/>
      <path d="M17.5 20h13M17.5 24.5h13M17.5 29h9.5" stroke="#8aa6c5" stroke-width="1.5" stroke-linecap="round"/>
      <path d="M17.5 34.5h6" stroke="#b8cbe0" stroke-width="1.5" stroke-linecap="round"/>
      <g fill="#1c4f96"><circle cx="13" cy="7.5" r="1.5"/><circle cx="19" cy="7.5" r="1.5"/><circle cx="25" cy="7.5" r="1.5"/><circle cx="31" cy="7.5" r="1.5"/><circle cx="37" cy="7.5" r="1.5" opacity="0"/></g>
      <rect x="9" y="10" width="3.5" height="29" fill="#0c3f86" opacity=".35"/>
    </symbol>

    <!-- Calculator -->
    <symbol id="app-calculator" viewBox="0 0 48 48">
      <rect x="9" y="5" width="30" height="38" rx="5" fill="url(#ig-calc)"/>
      <rect x="13" y="9.5" width="22" height="9" rx="1.8" fill="#0b2b52"/>
      <path d="M27 14.5h4.5" stroke="#d9f2ff" stroke-width="1.4" stroke-linecap="round"/>
      <g fill="#eaf6ff">
        <circle cx="16.5" cy="24.5" r="2.1"/><circle cx="24" cy="24.5" r="2.1"/><circle cx="31.5" cy="24.5" r="2.1"/>
        <circle cx="16.5" cy="31" r="2.1"/><circle cx="24" cy="31" r="2.1"/><circle cx="31.5" cy="31" r="2.1"/>
        <circle cx="16.5" cy="37.5" r="2.1"/><circle cx="24" cy="37.5" r="2.1"/>
      </g>
      <circle cx="31.5" cy="37.5" r="2.6" fill="#ffd166"/>
    </symbol>

    <!-- Terminal -->
    <symbol id="app-terminal" viewBox="0 0 48 48">
      <rect x="4" y="8" width="40" height="32" rx="5" fill="url(#ig-term)" stroke="#3b82f6" stroke-opacity=".35"/>
      <path d="M4 14.5c0-3.6 2.9-6.5 6.5-6.5h27c3.6 0 6.5 2.9 6.5 6.5V16H4v-1.5z" fill="#ffffff" opacity=".06"/>
      <g fill="#64748b" opacity=".8"><circle cx="10" cy="12.5" r="1.3"/><circle cx="14.5" cy="12.5" r="1.3"/><circle cx="19" cy="12.5" r="1.3"/></g>
      <path d="M13 24l5.5 4.5L13 33" fill="none" stroke="#7dd3fc" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M22 34h9" stroke="#e2e8f0" stroke-width="2.2" stroke-linecap="round"/>
      <circle cx="35.5" cy="29" r="2.2" fill="#22d3ee" opacity=".55"/>
    </symbol>

    <!-- Settings gear -->
    <symbol id="app-settings" viewBox="0 0 48 48">
      <g fill="url(#ig-gear)">
        <g transform="translate(24 24)">
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(45)"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(90)"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(135)"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(180)"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(225)"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(270)"/>
          <rect x="-3.6" y="-21" width="7.2" height="10" rx="2.2" transform="rotate(315)"/>
          <circle r="15.5"/>
        </g>
      </g>
      <circle cx="24" cy="24" r="7.5" fill="#eaf3ff"/>
      <circle cx="24" cy="24" r="4" fill="#7fa8d3"/>
    </symbol>

    <!-- Photos -->
    <symbol id="app-photos" viewBox="0 0 48 48">
      <rect x="7" y="7" width="34" height="34" rx="5" fill="url(#ig-paper)" stroke="#c9d6e4"/>
      <rect x="10" y="10" width="28" height="28" rx="3.2" fill="url(#ig-photo-sky)"/>
      <circle cx="31.5" cy="17.5" r="4" fill="#ffd166"/>
      <path d="M10 32l7.5-9 6.5 7 4.5-4.6L38 34v1H10v-3z" fill="#4cae9a"/>
      <path d="M10 35l9.5-6.5L26 34l5-4 7 5v1.5H10V35z" fill="#2c7a72"/>
      <rect x="7" y="7" width="34" height="34" rx="5" fill="none" stroke="#fff" stroke-opacity=".6"/>
    </symbol>

    <!-- Weather -->
    <symbol id="app-weather" viewBox="0 0 48 48">
      <rect x="7" y="7" width="34" height="34" rx="7" fill="url(#ig-weather)"/>
      <circle cx="31" cy="17" r="6.2" fill="#ffd35f"/>
      <g fill="#fff">
        <path d="M13 32.5c0-3.4 2.8-6.2 6.2-6.2 1 0 1.9.2 2.7.6 1.3-2.4 3.8-4 6.8-4 4.2 0 7.6 3.4 7.6 7.6v.4c1.9.6 3.2 2.4 3.2 4.4 0 2.6-2.1 4.7-4.7 4.7H17.2c-3.4 0-6.2-2.8-6.2-6.2 0-.5.1-.9.2-1.3H13z" opacity=".96"/>
      </g>
    </symbol>

    <!-- Recycle Bin -->
    <symbol id="app-recycle" viewBox="0 0 48 48">
      <path d="M12 15h24l-2 25.5a3.5 3.5 0 0 1-3.5 3.3H17.5A3.5 3.5 0 0 1 14 40.5L12 15z" fill="url(#ig-bin)" stroke="#7fa7c9" stroke-width="1.2"/>
      <path d="M10 13.5c0-1.4 1.1-2.5 2.5-2.5h23c1.4 0 2.5 1.1 2.5 2.5v1.6c0 1.4-1.1 2.4-2.5 2.4h-23A2.4 2.4 0 0 1 10 15.1v-1.6z" fill="#bcd6ea" stroke="#7fa7c9" stroke-width="1.2"/>
      <path d="M20 5h8" stroke="#7fa7c9" stroke-width="2.4" stroke-linecap="round"/>
      <g fill="none" stroke="#fff" stroke-width="1.4" opacity=".8"><path d="M17 20v19M24 20v20M31 20v19"/></g>
      <path d="M24 25.5l-4.2 7h8.4l-4.2-7z" fill="#41c99c" opacity=".95"/>
      <path d="M24 31.5v6" stroke="#0f9d76" stroke-width="1.6" stroke-linecap="round"/>
    </symbol>

    <!-- This PC -->
    <symbol id="app-thispc" viewBox="0 0 48 48">
      <rect x="4" y="8" width="40" height="27" rx="3.4" fill="url(#ig-metal)" stroke="#8ba3bb" stroke-width="1"/>
      <rect x="7" y="11" width="34" height="20" rx="2" fill="url(#ig-screen)"/>
      <path d="M7 16h34" stroke="#fff" stroke-opacity=".28"/>
      <path d="M18 35h12l2.5 6h-17L18 35z" fill="url(#ig-metal)"/>
      <rect x="12" y="41" width="24" height="2.6" rx="1.3" fill="#8ba3bb"/>
      <path d="M11 25l6-7 5 5 4-4.6 8.5 9.6H11z" fill="#b9e5ff" opacity=".45"/>
    </symbol>

    <!-- Folder -->
    <symbol id="app-folder" viewBox="0 0 48 48">
      <path d="M5 15.2C5 12.9 6.9 11 9.2 11h10.1l3.6 3.8h16.1c2.3 0 4.2 1.9 4.2 4.2v17.6c0 2.3-1.9 4.2-4.2 4.2H9.2A4.2 4.2 0 0 1 5 36.6V15.2z" fill="url(#ig-folder-back)"/>
      <path d="M5 25c0-2 1.6-3.6 3.6-3.6h31.8c2 0 3.6 1.6 3.6 3.6v11.4A3.6 3.6 0 0 1 40.4 40H8.6A3.6 3.6 0 0 1 5 36.4V25z" fill="url(#ig-folder-front)"/>
    </symbol>

    <!-- Text file -->
    <symbol id="app-textfile" viewBox="0 0 48 48">
      <path d="M10 6h19l9 9v27a2.5 2.5 0 0 1-2.5 2.5h-25A2.5 2.5 0 0 1 8 42V8.5A2.5 2.5 0 0 1 10.5 6z" fill="url(#ig-paper)" stroke="#c6d3e0" stroke-width="1"/>
      <path d="M29 6l9 9h-9V6z" fill="#cfe3f8"/>
      <path d="M15 21h18M15 26h18M15 31h18M15 36h11" stroke="#9db5cc" stroke-width="1.7" stroke-linecap="round"/>
    </symbol>

    <!-- Image file -->
    <symbol id="app-imagefile" viewBox="0 0 48 48">
      <path d="M10 6h19l9 9v27a2.5 2.5 0 0 1-2.5 2.5h-25A2.5 2.5 0 0 1 8 42V8.5A2.5 2.5 0 0 1 10.5 6z" fill="url(#ig-paper)" stroke="#c6d3e0" stroke-width="1"/>
      <path d="M29 6l9 9h-9V6z" fill="#cfe3f8"/>
      <rect x="14" y="21" width="20" height="14" rx="1.5" fill="#e8f4ff" stroke="#b8cfe4"/>
      <circle cx="27.5" cy="25" r="2.4" fill="#ffd166"/>
      <path d="M14 33l5-5 4 4 3-2.6 5 3.6v2H14v-2z" fill="#63bdae"/>
    </symbol>

    <!-- Drive -->
    <symbol id="app-drive" viewBox="0 0 48 48">
      <rect x="6" y="15" width="36" height="19" rx="3" fill="url(#ig-metal)" stroke="#8ba3bb"/>
      <rect x="9" y="19" width="30" height="11" rx="1.5" fill="#dce9f5" stroke="#b9cde0"/>
      <circle cx="34.5" cy="30" r="1.6" fill="#45c08e"/>
      <path d="M11 21.5h10M11 25h7" stroke="#92aec7" stroke-width="1.4" stroke-linecap="round"/>
    </symbol>

    <!-- User avatar glyph -->
    <symbol id="app-user" viewBox="0 0 48 48">
      <circle cx="24" cy="24" r="22" fill="#d7e8fb"/>
      <circle cx="24" cy="19" r="8" fill="#6f9cc9"/>
      <path d="M9 40c2.5-8 7.8-12 15-12s12.5 4 15 12" fill="#6f9cc9"/>
    </symbol>

    <!-- UI icons (24px stroke) -->
    <symbol id="ui-search" viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.4" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M15.3 15.3L21 21" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-taskview" viewBox="0 0 24 24"><rect x="3" y="6.5" width="13" height="11" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 4.5h10.5A2.5 2.5 0 0 1 21 7v9" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></symbol>
    <symbol id="ui-widgets" viewBox="0 0 24 24"><rect x="3" y="3" width="8" height="8" rx="2" fill="#4cc2ff"/><rect x="13" y="3" width="8" height="8" rx="2" fill="#7bdcb5"/><rect x="3" y="13" width="8" height="8" rx="2" fill="#ffd36e"/><rect x="13" y="13" width="8" height="8" rx="2" fill="#f6a6ff"/></symbol>
    <symbol id="ui-chevron-down" viewBox="0 0 24 24"><path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-chevron-right" viewBox="0 0 24 24"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-chevron-left" viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-chevron-up" viewBox="0 0 24 24"><path d="M6 15l6-6 6 6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-arrow-left" viewBox="0 0 24 24"><path d="M20 12H5m6-6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-arrow-right" viewBox="0 0 24 24"><path d="M4 12h15m-6-6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-up" viewBox="0 0 24 24"><path d="M12 20V5m-6 6l6-6 6 6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-refresh" viewBox="0 0 24 24"><path d="M20 11a8 8 0 1 0-2.3 5.7M20 5v6h-6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-plus" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-minus" viewBox="0 0 24 24"><path d="M5 12h14" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-close" viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></symbol>
    <symbol id="ui-menu" viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-grid" viewBox="0 0 24 24"><rect x="4" y="4" width="7" height="7" rx="1.2" fill="none" stroke="currentColor" stroke-width="1.5"/><rect x="13" y="4" width="7" height="7" rx="1.2" fill="none" stroke="currentColor" stroke-width="1.5"/><rect x="4" y="13" width="7" height="7" rx="1.2" fill="none" stroke="currentColor" stroke-width="1.5"/><rect x="13" y="13" width="7" height="7" rx="1.2" fill="none" stroke="currentColor" stroke-width="1.5"/></symbol>
    <symbol id="ui-list" viewBox="0 0 24 24"><path d="M9 6h11M9 12h11M9 18h11" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><circle cx="5" cy="6" r="1.2" fill="currentColor"/><circle cx="5" cy="12" r="1.2" fill="currentColor"/><circle cx="5" cy="18" r="1.2" fill="currentColor"/></symbol>
    <symbol id="ui-sort" viewBox="0 0 24 24"><path d="M7 4v16m-3-3l3 3 3-3M17 20V4m-3 3l3-3 3 3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-home" viewBox="0 0 24 24"><path d="M3.5 10.5L12 3l8.5 7.5V20a1 1 0 0 1-1 1h-5v-6h-5v6h-5a1 1 0 0 1-1-1v-9.5z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-folder" viewBox="0 0 24 24"><path d="M3 7.5A1.5 1.5 0 0 1 4.5 6H9l2 2h8.5A1.5 1.5 0 0 1 21 9.5v8a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5v-10z" fill="none" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-doc" viewBox="0 0 24 24"><path d="M6 3h8l4 4v14H6V3z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M14 3v5h5M9 12h6M9 16h6" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></symbol>
    <symbol id="ui-picture" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="16" cy="9" r="2" fill="currentColor"/><path d="M4 18l5-5 4 4 3-3 4 4" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-music" viewBox="0 0 24 24"><path d="M9 18V6l10-2v12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><circle cx="6.5" cy="18" r="2.5" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="16.5" cy="16" r="2.5" fill="none" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-video" viewBox="0 0 24 24"><rect x="3" y="5" width="13" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M16 10l5-3v10l-5-3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-download" viewBox="0 0 24 24"><path d="M12 3v12m-4-4l4 4 4-4M4 20h16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-desktop" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M9 20h6M12 16v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-pc" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M8 20h8M12 16v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><path d="M6 7h12" stroke="currentColor" stroke-width="1.2" opacity=".6"/></symbol>
    <symbol id="ui-trash" viewBox="0 0 24 24"><path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v5M14 11v5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-edit" viewBox="0 0 24 24"><path d="M4 20h4L20 8l-4-4L4 16v4z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M13.5 6.5l4 4" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-save" viewBox="0 0 24 24"><path d="M4 4h13l3 3v13H4V4z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M8 4v6h8V4M8 20v-6h8v6" fill="none" stroke="currentColor" stroke-width="1.5"/></symbol>
    <symbol id="ui-copy" viewBox="0 0 24 24"><rect x="8" y="8" width="12" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M16 8V5.5A1.5 1.5 0 0 0 14.5 4h-9A1.5 1.5 0 0 0 4 5.5v9A1.5 1.5 0 0 0 5.5 16H8" fill="none" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-cut" viewBox="0 0 24 24"><circle cx="6" cy="18" r="2.5" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="18" cy="18" r="2.5" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M7.5 16L18 4M16.5 16L6 4" fill="none" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-paste" viewBox="0 0 24 24"><path d="M9 5H6a1 1 0 0 0-1 1v14h14V6a1 1 0 0 0-1-1h-3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><rect x="9" y="3" width="6" height="4" rx="1" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 12h8M8 16h5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></symbol>
    <symbol id="ui-check" viewBox="0 0 24 24"><path d="M4 12.5l5 5L20 6.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-info" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M12 10v7M12 7.2v.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></symbol>
    <symbol id="ui-warning" viewBox="0 0 24 24"><path d="M12 3l9.5 17h-19L12 3z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M12 9v5M12 17.2v.2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-wifi" viewBox="0 0 24 24"><path d="M3 8.5C8 3.8 16 3.8 21 8.5M6 12.3c3.5-3.2 8.5-3.2 12 0M9.2 16c1.6-1.5 4-1.5 5.6 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><circle cx="12" cy="19.3" r="1.4" fill="currentColor"/></symbol>
    <symbol id="ui-wifi-off" viewBox="0 0 24 24"><path d="M3 8.5c2.5-2.3 5.5-3.5 8.5-3.5M15.5 6.2c2.2.6 4 1.8 5.5 3.3M6 12.3c1.2-1.1 2.7-1.8 4.2-2.2M12 19.3a1.4 1.4 0 1 0 0-.1M4 4l16 16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></symbol>
    <symbol id="ui-bluetooth" viewBox="0 0 24 24"><path d="M8 7l8 10-4 3V4l4 3-8 10" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-airplane" viewBox="0 0 24 24"><path d="M12 3c.8 0 1.5 1 1.5 2.2v4.6l7 4.2v2l-7-2v4.2l2.4 1.8V21l-3.9-1.2L8.1 21v-1l2.4-1.8V14l-7 2v-2l7-4.2V5.2C10.5 4 11.2 3 12 3z" fill="currentColor"/></symbol>
    <symbol id="ui-battery" viewBox="0 0 24 24"><rect x="2.5" y="7" width="18" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.6"/><rect x="21" y="10" width="1.8" height="4" rx=".8" fill="currentColor"/><rect x="4.5" y="9" width="13" height="6" rx="1" fill="currentColor" opacity=".9"/></symbol>
    <symbol id="ui-volume" viewBox="0 0 24 24"><path d="M4 9.5h3.5L12 6v12l-4.5-3.5H4v-5z" fill="currentColor"/><path d="M15.5 9a4.2 4.2 0 0 1 0 6M18.2 6.5a7.7 7.7 0 0 1 0 11" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-volume-mute" viewBox="0 0 24 24"><path d="M4 9.5h3.5L12 6v12l-4.5-3.5H4v-5z" fill="currentColor"/><path d="M16 9l5 6M21 9l-5 6" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-bell" viewBox="0 0 24 24"><path d="M6 16.5V11a6 6 0 1 1 12 0v5.5l1.5 2H4.5l1.5-2zM10 20.5a2 2 0 0 0 4 0" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-bell-off" viewBox="0 0 24 24"><path d="M6 16.5V11a6 6 0 0 1 9.3-5M18 12v4.5l1.5 2H4.5l1.5-2M10 20.5a2 2 0 0 0 4 0M3 3l18 18" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-moon" viewBox="0 0 24 24"><path d="M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-sun" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4.2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M18.7 5.3l-1.4 1.4M6.7 17.3l-1.4 1.4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-cloud" viewBox="0 0 24 24"><path d="M6.5 19a4.5 4.5 0 0 1-.6-8.96A6 6 0 0 1 17.6 9.1 4.5 4.5 0 0 1 17.5 19h-11z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-rain" viewBox="0 0 24 24"><path d="M6.5 15.5a4.5 4.5 0 0 1-.6-8.96A6 6 0 0 1 17.6 5.6 4.5 4.5 0 0 1 17.5 15.5h-11z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M8 18l-1.2 3M12.5 18l-1.2 3M17 18l-1.2 3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-wind" viewBox="0 0 24 24"><path d="M3 8h11a3 3 0 1 0-3-3M3 12h15a3 3 0 0 1 0 6h-3M3 16h6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-thermometer" viewBox="0 0 24 24"><path d="M10 13.8V5a2 2 0 1 1 4 0v8.8a4 4 0 1 1-4 0z" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M12 9v7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></symbol>
    <symbol id="ui-leaf" viewBox="0 0 24 24"><path d="M5 19C5 9 12 4 20 4c0 8-5 15-15 15z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M5 19c2-6 6-10 12-12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-access" viewBox="0 0 24 24"><circle cx="12" cy="4.5" r="2" fill="currentColor"/><path d="M4.5 8h15M12 8v5M12 13l-4 8M12 13l4 8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></symbol>
    <symbol id="ui-power" viewBox="0 0 24 24"><path d="M12 3v9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M7 6.5a8 8 0 1 0 10 0" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-restart" viewBox="0 0 24 24"><path d="M20 11a8 8 0 1 0-2.3 5.7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M20 4.5V10h-5.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-lock" viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="10.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M8 10V7.5a4 4 0 0 1 8 0V10M12 14v3" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-settings" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M19.4 13.5a7.8 7.8 0 0 0 0-3l2-1.6-2-3.4-2.5 1a8 8 0 0 0-2.6-1.5L14 2.5h-4l-.3 2.5a8 8 0 0 0-2.6 1.5l-2.5-1-2 3.4 2 1.6a7.8 7.8 0 0 0 0 3l-2 1.6 2 3.4 2.5-1a8 8 0 0 0 2.6 1.5l.3 2.5h4l.3-2.5a8 8 0 0 0 2.6-1.5l2.5 1 2-3.4-2-1.6z" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/></symbol>
    <symbol id="ui-user" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M4.5 21c.8-5 3.8-7.5 7.5-7.5s6.7 2.5 7.5 7.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-display" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="13" rx="2" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M8 21h8M12 17v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-palette" viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 0 0 18c1.3 0 2-1 2-2 0-1.2-1-2-1-3 0-1.4 1.2-2.2 2.6-2.2H18a3 3 0 0 0 3-3C21 6 17 3 12 3z" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="7.5" cy="10" r="1.2" fill="currentColor"/><circle cx="11" cy="7" r="1.2" fill="currentColor"/><circle cx="15.5" cy="8" r="1.2" fill="currentColor"/></symbol>
    <symbol id="ui-apps" viewBox="0 0 24 24"><rect x="3.5" y="3.5" width="7" height="7" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-privacy" viewBox="0 0 24 24"><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12l2 2 4-4.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-update" viewBox="0 0 24 24"><path d="M20 12a8 8 0 1 1-2.3-5.7" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M20 4v6h-6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/><path d="M12 8v5l3 2" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></symbol>
    <symbol id="ui-time" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M12 7v5.5l3.5 2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
    <symbol id="ui-pin" viewBox="0 0 24 24"><path d="M9 3h6l-1 6 4 4v2H6v-2l4-4-1-6zM12 15v6" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
    <symbol id="ui-more" viewBox="0 0 24 24"><circle cx="5" cy="12" r="1.5" fill="currentColor"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/><circle cx="19" cy="12" r="1.5" fill="currentColor"/></symbol>
    <symbol id="ui-calendar" viewBox="0 0 24 24"><rect x="3.5" y="5" width="17" height="15" rx="2" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3.5 9.5h17M8 3v4M16 3v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></symbol>
    <symbol id="ui-shield" viewBox="0 0 24 24"><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12l2 2 4-4.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></symbol>
    <symbol id="ui-globe" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3 12h18M12 3c2.8 2.4 4.2 5.4 4.2 9S14.8 18.6 12 21c-2.8-2.4-4.2-5.4-4.2-9S9.2 5.4 12 3z" fill="none" stroke="currentColor" stroke-width="1.4"/></symbol>
    <symbol id="ui-location" viewBox="0 0 24 24"><path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="10" r="2.4" fill="none" stroke="currentColor" stroke-width="1.6"/></symbol>
    <symbol id="ui-maximize" viewBox="0 0 24 24"><rect x="5.5" y="5.5" width="13" height="13" rx="1" fill="none" stroke="currentColor" stroke-width="1.4"/></symbol>
    <symbol id="ui-restore" viewBox="0 0 24 24"><rect x="4.5" y="8" width="11.5" height="11.5" rx="1" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M8 5.5h10.5V16" fill="none" stroke="currentColor" stroke-width="1.4"/></symbol>
    <symbol id="ui-layout-left" viewBox="0 0 24 24"><rect x="3" y="4" width="8" height="16" rx="1" fill="currentColor" opacity=".9"/><rect x="13" y="4" width="8" height="16" rx="1" fill="currentColor" opacity=".22"/></symbol>
    <symbol id="ui-layout-right" viewBox="0 0 24 24"><rect x="3" y="4" width="8" height="16" rx="1" fill="currentColor" opacity=".22"/><rect x="13" y="4" width="8" height="16" rx="1" fill="currentColor" opacity=".9"/></symbol>
    <symbol id="ui-max-window" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="1.5" fill="currentColor" opacity=".9"/></symbol>
  </svg>`;

  document.body.insertAdjacentHTML('afterbegin', sprite);

  window.Icon = {
    app(name, cls) {
      return `<svg class="app-icon ${cls || ''}" viewBox="0 0 48 48" aria-hidden="true"><use href="#app-${name}"></use></svg>`;
    },
    ui(name, cls) {
      return `<svg class="ui-icon ${cls || ''}" viewBox="0 0 24 24" aria-hidden="true"><use href="#ui-${name}"></use></svg>`;
    }
  };
  window.Win11 = window.Win11 || {};
  window.Win11.Icon = window.Icon;
})();
