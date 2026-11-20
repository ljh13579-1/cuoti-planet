/* Desktop shell behaviour: taskbar, flyouts, lock screen, power and settings. */
(function () {
  const Win11 = window.Win11;
  const U = Win11.util;
  const Icon = Win11.Icon;
  const fs = Win11.fs;
  const Store = Win11.Store;

  const $ = (id) => document.getElementById(id);
  const $$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));
  const WEEK = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  const MONTHS = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'];

  const UI = {};
  Win11.ui = UI;

  let openPanel = null;
  let taskviewOpen = false;
  let selectedDesktop = null;
  let currentMenu = null;
  let currentModal = null;
  let calCursor = null;
  let calSelected = null;
  let wallpaperA = null;
  let wallpaperB = null;
  let wallpaperActive = null;
  let currentWallpaperFile = '';
  let locked = true;
  let lockMenu = null;
  let accessibilityOn = false;
  let clockTimer = 0;

  const panelIds = {
    start: 'flyout-start',
    search: 'flyout-search',
    widgets: 'flyout-widgets',
    quick: 'flyout-quick',
    notif: 'flyout-notif'
  };

  function pad(n) { return String(n).padStart(2, '0'); }
  function fmtDate(d) { return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())}`; }
  function fmtLongDate(d) { return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${WEEK[d.getDay()]}`; }
  function relTime(d) { return d ? U.formatDateTime(d) : ''; }

  function hydrateStaticIcons() {
    $$('[data-icon]').forEach(span => {
      const name = span.dataset.icon.replace(/^ui-/, '');
      span.innerHTML = Icon.ui(name);
    });
  }

  /* ------------------------------- wallpaper ------------------------------- */
  function initWallpaper() {
    wallpaperA = $('wp-layer-a');
    wallpaperB = $('wp-layer-b');
    wallpaperActive = wallpaperA;
    const wall = Win11.wallpaperOf(Store.state.wallpaper);
    wallpaperA.style.backgroundImage = `url("${wall.file}")`;
    wallpaperB.style.backgroundImage = `url("${wall.file}")`;
    wallpaperA.classList.add('active');
    wallpaperB.classList.remove('active');
    currentWallpaperFile = wall.file;
    $('lock-bg').style.backgroundImage = `url("${wall.file}")`;
  }

  function updateWallpaper() {
    const wall = Win11.wallpaperOf(Store.state.wallpaper);
    if (wall.file === currentWallpaperFile) {
      $('lock-bg').style.backgroundImage = `url("${wall.file}")`;
      return;
    }
    const next = wallpaperActive === wallpaperA ? wallpaperB : wallpaperA;
    const prev = wallpaperActive;
    next.style.backgroundImage = `url("${wall.file}")`;
    next.classList.add('active');
    prev.classList.remove('active');
    wallpaperActive = next;
    currentWallpaperFile = wall.file;
    $('lock-bg').style.backgroundImage = `url("${wall.file}")`;
    if (openPanel === 'widgets') renderWidgets();
  }

  function applyOverlays() {
    const b = Number(Store.state.brightness);
    $('brightness-overlay').style.opacity = String(Math.max(0, Math.min(.58, (100 - b) / 100 * .62)));
    $('night-overlay').style.opacity = Store.state.nightLight ? '.17' : '0';
  }

  /* --------------------------------- toast --------------------------------- */
  function toast(title, body, opts) {
    opts = opts || {};
    if (Store.state.dnd && !opts.force) return;
    const layer = $('toast-layer');
    const el = document.createElement('div');
    el.className = 'toast';
    el.setAttribute('role', 'status');
    let iconHtml;
    if (typeof opts.icon === 'string' && opts.icon.trim().startsWith('<svg')) iconHtml = opts.icon;
    else if (typeof opts.icon === 'string') iconHtml = Icon.ui(opts.icon);
    else iconHtml = Icon.ui('info');
    el.innerHTML = `
      <div class="toast-icon">${iconHtml}</div>
      <div class="toast-main">
        <div class="toast-title">${U.escapeHtml(title || '通知')}</div>
        ${body ? `<div class="toast-body">${U.escapeHtml(body)}</div>` : ''}
      </div>
      <button class="toast-close" aria-label="关闭通知">${Icon.ui('close')}</button>
    `;
    layer.appendChild(el);
    while (layer.children.length > 3) layer.firstElementChild.remove();
    let timer = 0;
    const close = () => {
      clearTimeout(timer);
      if (el.classList.contains('leaving')) return;
      el.classList.add('leaving');
      setTimeout(() => el.remove(), 220);
    };
    const arm = () => {
      clearTimeout(timer);
      timer = setTimeout(close, opts.duration || 5200);
    };
    arm();
    el.querySelector('.toast-close').addEventListener('click', close);
    el.addEventListener('mouseenter', () => clearTimeout(timer));
    el.addEventListener('mouseleave', arm);
  }

  /* ----------------------------- context menus ----------------------------- */
  function closeMenu() {
    if (currentMenu) {
      currentMenu.remove();
      currentMenu = null;
    }
  }

  function clampMenu(menu, x, y) {
    const pad = 8;
    const w = menu.offsetWidth, h = menu.offsetHeight;
    const nx = Math.max(pad, Math.min(x, window.innerWidth - w - pad));
    const ny = Math.max(pad, Math.min(y, window.innerHeight - h - pad));
    menu.style.left = nx + 'px';
    menu.style.top = ny + 'px';
  }

  function buildMenu(items, x, y) {
    closeMenu();
    const menu = document.createElement('div');
    menu.className = 'context-menu';
    menu.setAttribute('role', 'menu');
    const html = items.map((item, i) => {
      if (item.separator) return '<div class="ctx-sep"></div>';
      const icon = item.icon ? Icon.ui(item.icon) : '';
      const check = item.checked !== undefined ? `<span class="ctx-check">${item.checked ? Icon.ui('check') : ''}</span>` : '';
      return `<button class="ctx-item ${item.danger ? 'danger' : ''} ${item.disabled ? 'disabled' : ''} ${item.checked ? 'checked' : ''}" data-mi="${i}" ${item.disabled ? 'disabled' : ''}>
        ${check}${!check ? `<span class="ctx-icon-space">${icon || ''}</span>` : ''}
        <span class="ctx-label">${U.escapeHtml(item.label || '')}</span>
        ${item.shortcut ? `<span class="ctx-shortcut">${U.escapeHtml(item.shortcut)}</span>` : ''}
      </button>`;
    }).join('');
    menu.innerHTML = html;
    $('context-layer').appendChild(menu);
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';
    menu.style.visibility = 'hidden';
    currentMenu = menu;
    clampMenu(menu, x, y);
    menu.style.visibility = '';
    menu.addEventListener('pointerdown', e => e.stopPropagation());
    menu.addEventListener('contextmenu', e => e.preventDefault());
    menu.addEventListener('click', e => {
      const btn = e.target.closest('[data-mi]');
      if (!btn || btn.classList.contains('disabled')) return;
      const item = items[Number(btn.dataset.mi)];
      closeMenu();
      if (item && typeof item.action === 'function') setTimeout(() => item.action(), 0);
    });
    return menu;
  }

  function contextMenu(x, y, items) {
    return buildMenu(items, x, y);
  }

  function showMenu(anchor, items, placement) {
    placement = placement || 'bottom-start';
    const r = anchor.getBoundingClientRect();
    let x = r.left;
    let y = r.bottom + 4;
    const menu = buildMenu(items, x, y);
    // Re-place once measured.
    if (placement.includes('end')) x = r.right - menu.offsetWidth;
    if (placement.includes('top')) y = r.top - menu.offsetHeight - 4;
    clampMenu(menu, x, y);
    return menu;
  }

  document.addEventListener('pointerdown', e => {
    if (currentMenu && !e.target.closest('.context-menu')) closeMenu();
  }, true);
  window.addEventListener('resize', closeMenu);

  /* -------------------------------- modals --------------------------------- */
  function closeModal() {
    if (currentModal) {
      currentModal.remove();
      currentModal = null;
    }
  }

  function modalMarkup(title, message, inner, buttons) {
    return `<div class="modal" role="dialog" aria-modal="true">
      <h3>${U.escapeHtml(title)}</h3>
      ${message ? `<p>${U.escapeHtml(message)}</p>` : ''}
      ${inner || ''}
      <div class="modal-buttons">${buttons}</div>
    </div>`;
  }

  function showModal(innerHtml, onReady) {
    closeModal();
    closeMenu();
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop';
    backdrop.innerHTML = innerHtml;
    document.body.appendChild(backdrop);
    currentModal = backdrop;
    onReady(backdrop);
    return backdrop;
  }

  function prompt(opts) {
    showModal(modalMarkup(
      opts.title || '输入',
      opts.message || '',
      `<input type="text" id="modal-input" value="${U.escapeHtml(opts.value || '')}" aria-label="${U.escapeHtml(opts.label || '输入')}">`,
      `<button class="secondary-btn" data-modal="cancel">取消</button><button class="primary-btn" data-modal="ok">${U.escapeHtml(opts.okText || '确定')}</button>`
    ), backdrop => {
      const input = backdrop.querySelector('#modal-input');
      const submit = () => {
        const v = input.value;
        closeModal();
        if (typeof opts.onOk === 'function') opts.onOk(v);
      };
      backdrop.querySelector('[data-modal="cancel"]').addEventListener('click', closeModal);
      backdrop.querySelector('[data-modal="ok"]').addEventListener('click', submit);
      input.addEventListener('keydown', e => {
        if (e.key === 'Enter') { e.preventDefault(); submit(); }
        if (e.key === 'Escape') { e.preventDefault(); closeModal(); }
      });
      backdrop.addEventListener('keydown', e => {
        if (e.key === 'Escape') { e.preventDefault(); closeModal(); }
      });
      setTimeout(() => { input.focus(); input.select(); }, 30);
    });
  }

  function confirm(opts) {
    showModal(modalMarkup(
      opts.title || '确认',
      opts.message || '',
      '',
      `<button class="secondary-btn" data-modal="cancel">取消</button><button class="primary-btn ${opts.danger ? 'danger-btn' : ''}" data-modal="ok">${U.escapeHtml(opts.okText || '确定')}</button>`
    ), backdrop => {
      const accept = () => {
        closeModal();
        if (typeof opts.onOk === 'function') opts.onOk();
      };
      backdrop.querySelector('[data-modal="cancel"]').addEventListener('click', closeModal);
      backdrop.querySelector('[data-modal="ok"]').addEventListener('click', accept);
      backdrop.addEventListener('keydown', e => {
        if (e.key === 'Escape') { e.preventDefault(); closeModal(); }
        if (e.key === 'Enter') { e.preventDefault(); accept(); }
      });
      setTimeout(() => backdrop.querySelector('[data-modal="ok"]').focus(), 30);
    });
  }

  /* ------------------------------- flyouts --------------------------------- */
  function flyoutEl(name) { return $(panelIds[name]); }

  function closeAllFlyouts() {
    $$('.flyout').forEach(panel => {
      if (panel.classList.contains('open')) {
        panel.classList.remove('open');
        setTimeout(() => { if (!panel.classList.contains('open')) panel.hidden = true; }, 220);
      }
    });
    $$('[data-flyout-trigger]').forEach(btn => btn.classList.remove('open'));
    openPanel = null;
    updateNotifDot();
  }

  function openFlyout(name) {
    if (openPanel === name) { closeAllFlyouts(); return; }
    closeAllFlyouts();
    if (taskviewOpen) closeTaskview();
    const panel = flyoutEl(name);
    if (!panel) return;
    if (name === 'start') renderStart();
    if (name === 'search') renderSearch();
    if (name === 'widgets') renderWidgets();
    if (name === 'quick') renderQuick();
    if (name === 'notif') renderNotifications();
    openPanel = name;
    panel.hidden = false;
    requestAnimationFrame(() => panel.classList.add('open'));
    $$(`[data-flyout-trigger="${name}"]`).forEach(btn => btn.classList.add('open'));
    if (name === 'notif') updateNotifDot();
    setTimeout(() => {
      if (name === 'start') {
        const input = panel.querySelector('#start-search-input');
        if (input) input.focus();
      }
      if (name === 'search') {
        const input = panel.querySelector('#search-input');
        if (input) { input.focus(); input.select(); }
      }
    }, 70);
  }

  function openSearchPanel() {
    if (openPanel === 'search') {
      const input = flyoutEl('search').querySelector('#search-input');
      if (input) { input.focus(); input.select(); }
      return;
    }
    openFlyout('search');
  }

  document.addEventListener('pointerdown', e => {
    if (!openPanel) return;
    if (e.target.closest('.flyout') || e.target.closest('.context-menu') || e.target.closest('.modal-backdrop')) return;
    if (e.target.closest('[data-flyout-trigger]')) return;
    closeAllFlyouts();
  });

  document.addEventListener('win11:settings', e => {
    const detail = e.detail || {};
    const key = detail.key;
    const reset = !!detail.reset;
    if (reset || key === 'wallpaper') updateWallpaper();
    if (reset || ['brightness', 'nightLight'].includes(key)) applyOverlays();
    if (reset || ['wifi', 'airplane', 'volume', 'dnd', 'bluetooth'].includes(key)) renderTrayIcons();
    if (reset || key === 'userName') refreshIdentity();
  });

  UI.toast = toast;
  UI.contextMenu = contextMenu;
  UI.showMenu = showMenu;
  UI.prompt = prompt;
  UI.confirm = confirm;
  UI.closeAllFlyouts = closeAllFlyouts;
  UI.openApp = function (appId, opts) {
    closeAllFlyouts();
    closeTaskview();
    Win11.WM.open(appId, opts);
  };

  /* ------------------------------- identity -------------------------------- */
  function refreshIdentity() {
    const name = Win11.userName();
    const initial = Win11.initialOf(name);
    const avatar = $('signin-avatar');
    const nameEl = $('signin-name');
    if (avatar) avatar.textContent = initial;
    if (nameEl) nameEl.textContent = name;
    $$('.start-user .avatar, .widgets-head .avatar').forEach(el => { el.textContent = initial; });
    document.title = `Windows 11 · ${name}`;
  }

  /* -------------------------------- taskbar -------------------------------- */
  function windowsOf(appId) {
    return Win11.WM.windows.filter(w => w.appId === appId && !w.closed);
  }

  function renderTaskbar() {
    const wrap = $('tb-apps');
    if (!wrap) return;
    const wins = Win11.WM.windows.filter(w => !w.closed);
    const order = Win11.taskbarOrder.slice();
    wins.forEach(w => { if (!order.includes(w.appId)) order.push(w.appId); });
    wrap.innerHTML = order.map(id => {
      const meta = Win11.appsMeta[id];
      if (!meta) return '';
      const appWins = wins.filter(w => w.appId === id);
      const running = appWins.length > 0;
      const focused = appWins.some(w => w.active && !w.minimized);
      const label = running
        ? `${meta.name}${appWins.length > 1 ? ` (${appWins.length})` : ''}`
        : meta.name;
      return `<button class="tb-app ${running ? 'running' : ''} ${focused ? 'focused' : ''}"
        data-taskbar-app="${id}" title="${U.escapeHtml(label)}" aria-label="${U.escapeHtml(label)}">${Icon.app(meta.icon)}</button>`;
    }).join('');
  }

  function focusWindow(w) {
    if (!w || w.closed) return;
    closeTaskview();
    Win11.WM.focus(w);
  }

  function onTaskbarAppClick(appId) {
    const wins = windowsOf(appId);
    if (!wins.length) {
      UI.openApp(appId);
      return;
    }
    const focused = wins.find(w => w.active && !w.minimized);
    if (focused) {
      Win11.WM.minimize(focused);
      return;
    }
    const recent = wins.slice().sort((a, b) => (b.z || 0) - (a.z || 0))[0];
    focusWindow(recent);
  }

  function showTaskbarAppMenu(appId, anchor) {
    const meta = Win11.appsMeta[appId];
    if (!meta) return;
    const wins = windowsOf(appId).slice().sort((a, b) => (b.z || 0) - (a.z || 0));
    const items = [];
    wins.forEach(w => {
      items.push({
        label: w.title || meta.name,
        icon: 'max-window',
        checked: w.active && !w.minimized,
        action: () => focusWindow(w)
      });
    });
    if (wins.length) items.push({ separator: true });
    if (wins.length) {
      items.push({
        label: '关闭所有窗口', icon: 'close', danger: true,
        action: () => wins.slice().forEach(w => Win11.WM.close(w))
      });
    }
    if (!meta.single && wins.length) {
      items.push({ label: '新建窗口', icon: 'plus', action: () => UI.openApp(appId) });
    }
    if (!wins.length) {
      items.push({ label: '打开', icon: meta.icon, action: () => UI.openApp(appId) });
    }
    showMenu(anchor, items, 'top-start');
  }

  function minimizeAllWindows() {
    Win11.WM.windows.filter(w => !w.closed && !w.minimized).forEach(w => Win11.WM.minimize(w));
  }

  function openSettings(section) {
    closeAllFlyouts();
    closeTaskview();
    Win11.WM.open('settings', { section: section || 'system' });
  }

  function setupTaskbar() {
    const apps = $('tb-apps');
    if (apps) {
      apps.addEventListener('click', e => {
        const btn = e.target.closest('[data-taskbar-app]');
        if (!btn) return;
        onTaskbarAppClick(btn.dataset.taskbarApp);
      });
      apps.addEventListener('contextmenu', e => {
        const btn = e.target.closest('[data-taskbar-app]');
        if (!btn) return;
        e.preventDefault();
        showTaskbarAppMenu(btn.dataset.taskbarApp, btn);
      });
    }

    const taskviewBtn = $('tb-taskview');
    if (taskviewBtn) taskviewBtn.addEventListener('click', () => toggleTaskview());

    const taskbar = $('taskbar');
    if (taskbar) {
      taskbar.addEventListener('contextmenu', e => {
        if (e.target.closest('[data-taskbar-app]')) return;
        e.preventDefault();
        contextMenu(e.clientX, e.clientY, [
          { label: '任务栏设置', icon: 'settings', action: () => openSettings('personalization') },
          { label: '任务视图', icon: 'taskview', action: () => toggleTaskview() },
          { separator: true },
          { label: '显示桌面', icon: 'desktop', action: minimizeAllWindows }
        ]);
      });
    }
  }

  /* -------------------------------- desktop -------------------------------- */
  function desktopVirtualItems() {
    return [
      { key: 'thispc', name: '此电脑', virtual: 'thispc', icon: 'thispc' },
      { key: 'recycle', name: '回收站', virtual: 'recycle', icon: 'recycle' }
    ];
  }

  function nodeAppIcon(node) {
    if (!node) return Icon.app('textfile');
    if (node.type === 'drive') return Icon.app('drive');
    if (node.type === 'folder') return Icon.app('folder');
    if (node.kind === 'image') return Icon.app('imagefile');
    return Icon.app('textfile');
  }

  function desktopItemIcon(item) {
    if (item.virtual) return Icon.app(item.icon);
    return nodeAppIcon(item.node);
  }

  function renderDesktop() {
    const wrap = $('desktop-icons');
    if (!wrap) return;
    const desktop = fs.find('desktop');
    const items = desktopVirtualItems();
    ((desktop && desktop.children) || []).forEach(node => {
      items.push({ key: node.id, name: node.name, node });
    });
    if (selectedDesktop && !items.some(item => item.key === selectedDesktop)) selectedDesktop = null;
    wrap.innerHTML = items.map(item => `
      <div class="desk-icon ${selectedDesktop === item.key ? 'selected' : ''}" data-desk-key="${U.escapeHtml(item.key)}" title="${U.escapeHtml(item.name)}" role="button" tabindex="0">
        ${desktopItemIcon(item)}
        <span class="desk-icon-label">${U.escapeHtml(item.name)}</span>
      </div>
    `).join('');
  }

  function refreshDesktop(animate) {
    renderDesktop();
    const wrap = $('desktop-icons');
    if (!wrap || animate === false) return;
    wrap.classList.remove('refreshing');
    void wrap.offsetWidth;
    wrap.classList.add('refreshing');
  }

  function selectDesktopKey(key) {
    selectedDesktop = key;
    $$('.desk-icon', $('desktop-icons')).forEach(el => {
      el.classList.toggle('selected', el.dataset.deskKey === key);
    });
  }

  function desktopEntry(key) {
    if (!key) return null;
    const special = desktopVirtualItems().find(item => item.key === key);
    if (special) return special;
    const node = fs.find(key);
    const desktop = fs.find('desktop');
    if (!node || !desktop || fs.parentOf(node) !== desktop) return null;
    return { key: node.id, name: node.name, node };
  }

  function openDesktopEntry(entry) {
    if (!entry) return;
    if (entry.virtual === 'thispc') { UI.openApp('explorer', { nodeId: 'thispc' }); return; }
    if (entry.virtual === 'recycle') { UI.openApp('recycle'); return; }
    if (entry.node) Win11.openFsNode(entry.node);
  }

  function nodeProperties(node) {
    const type = node.type === 'folder'
      ? `文件夹 · ${(node.children || []).length} 个项目`
      : node.type === 'drive' ? '磁盘驱动器' : node.kind === 'image' ? 'SVG 图像' : '文本文件';
    const size = node.type === 'folder' || node.type === 'drive' ? '' : ` · ${U.formatSize(node.size)}`;
    toast(node.name, `${type}${size} · 修改于 ${node.modified}`, { icon: nodeAppIcon(node) });
  }

  function renameNode(node) {
    if (!node) return;
    UI.prompt({
      title: '重命名',
      message: `为「${node.name}」输入新名称。`,
      label: '名称',
      value: node.name,
      okText: '重命名',
      onOk: value => {
        const name = String(value || '').trim();
        if (!name || name === node.name) return;
        node.name = name;
        refreshDesktop();
        toast('已重命名', name, { icon: nodeAppIcon(node) });
      }
    });
  }

  function deleteNode(node) {
    if (!node) return;
    const parent = fs.parentOf(node);
    if (!parent) {
      toast('无法删除', '这个位置受保护，不能删除。');
      return;
    }
    fs.remove(node);
    Win11.recycle.unshift({
      node, parentId: parent.id, parentName: parent.name, at: new Date()
    });
    refreshDesktop();
    if (typeof Win11.refreshRecycleWindows === 'function') Win11.refreshRecycleWindows();
    toast('已移到回收站', `${node.name} 已移到回收站。`, { icon: Icon.app('recycle') });
  }

  function uniqueChildName(base, ext, parent) {
    const names = new Set(((parent && parent.children) || []).map(child => child.name));
    let name = base + (ext || '');
    let i = 2;
    while (names.has(name)) {
      name = `${base} (${i})${ext || ''}`;
      i += 1;
    }
    return name;
  }

  function createDesktopFolder() {
    const desktop = fs.find('desktop');
    if (!desktop) return;
    const name = uniqueChildName('新建文件夹', '', desktop);
    const node = fs.createFolder(name, desktop);
    refreshDesktop();
    selectDesktopKey(node.id);
    toast('已新建文件夹', name, { icon: Icon.app('folder') });
  }

  function createDesktopText() {
    const desktop = fs.find('desktop');
    if (!desktop) return;
    const name = uniqueChildName('新建文本文档', '.txt', desktop);
    const node = fs.createText(name, '', desktop);
    refreshDesktop();
    selectDesktopKey(node.id);
    toast('已新建文本文档', name, { icon: Icon.app('textfile') });
  }

  function desktopEntryMenu(entry, x, y) {
    const items = [];
    if (!entry) {
      showDesktopMenu(x, y);
      return;
    }
    if (entry.virtual) {
      items.push({ label: '打开', icon: entry.virtual === 'recycle' ? 'trash' : 'pc', action: () => openDesktopEntry(entry) });
      items.push({ separator: true });
      items.push({
        label: '属性', icon: 'info',
        action: () => toast(entry.name, entry.virtual === 'recycle'
          ? `${Win11.recycle.length} 个已删除项目`
          : '查看设备和驱动器', { icon: Icon.app(entry.icon) })
      });
    } else if (entry.node) {
      const node = entry.node;
      const isFolder = node.type === 'folder' || node.type === 'drive';
      items.push({
        label: '打开',
        icon: isFolder ? 'folder' : node.kind === 'image' ? 'picture' : 'doc',
        action: () => openDesktopEntry(entry)
      });
      const wall = node.kind === 'image' ? Win11.wallpapers.find(w => w.file === node.image) : null;
      if (wall) {
        items.push({
          label: '设为桌面背景', icon: 'display',
          action: () => {
            Store.set('wallpaper', wall.id);
            toast('桌面背景已更换', `正在使用「${wall.name}」。`, { icon: Icon.app('photos') });
          }
        });
      }
      items.push({ separator: true });
      items.push({ label: '重命名', icon: 'edit', action: () => renameNode(node) });
      items.push({ label: '删除', icon: 'trash', danger: true, action: () => deleteNode(node) });
      items.push({ separator: true });
      items.push({ label: '属性', icon: 'info', action: () => nodeProperties(node) });
    }
    contextMenu(x, y, items);
  }

  function showDesktopMenu(x, y) {
    contextMenu(x, y, [
      { label: '刷新', icon: 'refresh', action: () => refreshDesktop() },
      { separator: true },
      { label: '新建文件夹', icon: 'folder', action: createDesktopFolder },
      { label: '新建文本文档', icon: 'doc', action: createDesktopText },
      { separator: true },
      { label: '更改桌面背景', icon: 'picture', action: () => openSettings('personalization') },
      { label: '个性化', icon: 'palette', action: () => openSettings('personalization') },
      { label: '显示设置', icon: 'display', action: () => openSettings('system') }
    ]);
  }

  function setupDesktop() {
    const icons = $('desktop-icons');
    if (icons) {
      icons.addEventListener('click', e => {
        const el = e.target.closest('.desk-icon');
        if (!el) { selectDesktopKey(null); return; }
        selectDesktopKey(el.dataset.deskKey);
      });
      icons.addEventListener('dblclick', e => {
        const el = e.target.closest('.desk-icon');
        if (!el) return;
        openDesktopEntry(desktopEntry(el.dataset.deskKey));
      });
      icons.addEventListener('keydown', e => {
        const el = e.target.closest('.desk-icon');
        if (!el) return;
        if (e.key === 'Enter') { e.preventDefault(); openDesktopEntry(desktopEntry(el.dataset.deskKey)); }
        if (e.key === 'Delete') { e.preventDefault(); const entry = desktopEntry(el.dataset.deskKey); if (entry && entry.node) deleteNode(entry.node); }
      });
      icons.addEventListener('contextmenu', e => {
        const el = e.target.closest('.desk-icon');
        if (!el) return;
        e.preventDefault();
        e.stopPropagation();
        selectDesktopKey(el.dataset.deskKey);
        desktopEntryMenu(desktopEntry(el.dataset.deskKey), e.clientX, e.clientY);
      });
    }

    const desktop = $('desktop');
    if (desktop) {
      desktop.addEventListener('click', e => {
        if (e.target.closest('#desktop-icons') || e.target.closest('.window') || e.target.closest('#taskview')) return;
        selectDesktopKey(null);
      });
      desktop.addEventListener('contextmenu', e => {
        if (e.target.closest('#desktop-icons') || e.target.closest('.window') || e.target.closest('#taskview')) return;
        e.preventDefault();
        selectDesktopKey(null);
        showDesktopMenu(e.clientX, e.clientY);
      });
    }
  }

  /* --------------------------------- clock --------------------------------- */
  function updateClock() {
    const now = new Date();
    const time = `${pad(now.getHours())}:${pad(now.getMinutes())}`;
    const trayTime = $('tray-time');
    const trayDate = $('tray-date');
    const lockTime = $('lock-time');
    const lockDate = $('lock-date');
    if (trayTime) trayTime.textContent = time;
    if (trayDate) trayDate.textContent = `${now.getFullYear()}/${now.getMonth() + 1}/${now.getDate()}`;
    if (lockTime) lockTime.textContent = time;
    if (lockDate) lockDate.textContent = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${WEEK[now.getDay()]}`;
  }

  function updateNotifDot() {
    const dot = $('notif-dot');
    if (!dot) return;
    dot.hidden = !Win11.notifications.length || openPanel === 'notif';
  }

  function renderTrayIcons() {
    const s = Store.state;
    const quick = $('tb-quick');
    if (quick) {
      const wifi = s.airplane ? 'airplane' : s.wifi ? 'wifi' : 'wifi-off';
      const volume = Number(s.volume) <= 0 ? 'volume-mute' : 'volume';
      quick.innerHTML = `${Icon.ui(wifi)}${Icon.ui(volume)}${Icon.ui('battery')}`;
      quick.title = s.airplane ? '飞行模式已开启' : s.wifi ? '已连接到 Contoso Wi-Fi' : '网络已断开';
    }
    const bellSlot = $('tb-notif') && $('tb-notif').querySelector('[data-icon]');
    if (bellSlot) bellSlot.innerHTML = Icon.ui(s.dnd ? 'bell-off' : 'bell');
    const bell = $('tb-notif');
    if (bell) bell.title = s.dnd ? '通知已关闭' : '通知';
    const weatherIcon = $('weather-mini');
    if (weatherIcon) weatherIcon.innerHTML = Icon.ui('sun');
    const weatherTemp = $('weather-mini-temp');
    if (weatherTemp) weatherTemp.textContent = `${Win11.weather.temp}°`;
    updateNotifDot();
  }

  function setupFlyoutTriggers() {
    $$('[data-flyout-trigger]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.preventDefault();
        openFlyout(btn.dataset.flyoutTrigger);
      });
    });
  }

  /* -------------------------------- search data ---------------------------- */
  function collectFileNodes() {
    const list = [];
    const walk = node => {
      (node.children || []).forEach(child => {
        list.push(child);
        if (child.children) walk(child);
      });
    };
    walk(fs.root);
    return list;
  }

  function filePathString(node) {
    return fs.pathOf(node).map(n => n.name).join(' \\ ');
  }

  function fileTypeLabel(node) {
    if (node.type === 'folder') return '文件夹';
    if (node.type === 'drive') return '磁盘驱动器';
    if (node.kind === 'image') return 'SVG 图像';
    return '文本文档';
  }

  function fileSubText(node) {
    const parent = fs.parentOf(node);
    if (node.type === 'folder') return `文件夹 · ${parent ? parent.name : '此电脑'}`;
    if (node.kind === 'image') return `图片 · ${U.formatSize(node.size)}`;
    return `文本文档 · ${parent ? parent.name : '此电脑'}`;
  }

  function searchAll(query) {
    const q = String(query || '').trim().toLowerCase();
    if (!q) return { apps: [], files: [] };
    const apps = Win11.startOrder.filter(id => {
      const meta = Win11.appsMeta[id];
      if (!meta) return false;
      const haystack = [meta.name, id].concat(meta.keywords || []).join(' ').toLowerCase();
      return haystack.includes(q);
    }).map(id => ({ id, meta: Win11.appsMeta[id] }));
    const files = collectFileNodes().filter(node => {
      const haystack = `${node.name} ${filePathString(node)}`.toLowerCase();
      return haystack.includes(q);
    }).slice(0, 12);
    return { apps, files };
  }

  function recommendedFileMarkup(compact) {
    return Win11.recommended.map(rec => {
      const node = fs.find(rec.fileId);
      if (!node) return '';
      const icon = node.type === 'folder' ? Icon.app('folder') : node.kind === 'image' ? Icon.app('imagefile') : Icon.app('textfile');
      if (compact) {
        return `<button class="search-result" data-search-result data-open-file="${U.escapeHtml(node.id)}">
          ${icon}
          <span class="search-result-main"><span class="search-result-name">${U.escapeHtml(node.name)}</span><span class="search-result-sub">${U.escapeHtml(filePathString(node))}</span></span>
          <span class="search-meta">${U.escapeHtml(fileTypeLabel(node))}</span>
        </button>`;
      }
      return `<button class="rec-item" data-open-file="${U.escapeHtml(node.id)}">
        <span class="rec-thumb">${icon}</span>
        <span class="rec-main"><span class="rec-name">${U.escapeHtml(node.name)}</span><span class="rec-sub">${U.escapeHtml(fileSubText(node))}</span></span>
      </button>`;
    }).join('');
  }

  /* --------------------------------- start --------------------------------- */
  function pinnedTilesMarkup() {
    return Win11.startOrder.map(id => {
      const meta = Win11.appsMeta[id];
      if (!meta) return '';
      return `<button class="pin-tile" data-open-app="${id}" title="${U.escapeHtml(meta.name)}">
        ${Icon.app(meta.icon)}<span>${U.escapeHtml(meta.name)}</span>
      </button>`;
    }).join('');
  }

  function startResultsMarkup(query) {
    const q = String(query || '').trim();
    const res = searchAll(q);
    if (!res.apps.length && !res.files.length) {
      return `<div class="start-empty">没有找到与「${U.escapeHtml(q)}」相关的内容。<br>试试搜索“记事本”或“项目”。</div>`;
    }
    let html = '<div class="start-section-head"><span>搜索结果</span></div>';
    if (res.apps.length) {
      html += '<div class="search-results">' + res.apps.map(app => `
        <button class="search-result" data-search-result data-open-app="${app.id}">
          ${Icon.app(app.meta.icon)}
          <span class="search-result-main"><span class="search-result-name">${U.escapeHtml(app.meta.name)}</span><span class="search-result-sub">应用</span></span>
          <span class="search-meta">应用</span>
        </button>`).join('') + '</div>';
    }
    if (res.files.length) {
      html += '<div class="start-section-head"><span>文件</span></div><div class="search-results">' + res.files.map(node => `
        <button class="search-result" data-search-result data-open-file="${U.escapeHtml(node.id)}">
          ${nodeAppIcon(node)}
          <span class="search-result-main"><span class="search-result-name">${U.escapeHtml(node.name)}</span><span class="search-result-sub">${U.escapeHtml(filePathString(node))}</span></span>
          <span class="search-meta">${U.escapeHtml(fileTypeLabel(node))}</span>
        </button>`).join('') + '</div>';
    }
    return html;
  }

  function paintStartBody(query) {
    const scroll = $('start-scroll');
    if (!scroll) return;
    const q = String(query || '').trim();
    if (q) {
      scroll.innerHTML = startResultsMarkup(q);
      return;
    }
    scroll.innerHTML = `
      <div class="start-section-head"><span>已固定</span><button data-start-all>所有应用 ${Icon.ui('chevron-right')}</button></div>
      <div class="pinned-grid">${pinnedTilesMarkup()}</div>
      <div class="start-section-head"><span>推荐的项目</span><button data-start-more>更多 ${Icon.ui('chevron-right')}</button></div>
      <div class="rec-grid">${recommendedFileMarkup(false)}</div>
    `;
  }

  function showAllApps() {
    const panel = flyoutEl('start');
    if (!panel || panel.querySelector('.start-all')) return;
    const all = document.createElement('div');
    all.className = 'start-all';
    all.innerHTML = `
      <div class="start-all-head"><button class="start-back" aria-label="返回">${Icon.ui('arrow-left')}</button><span>所有应用</span></div>
      <div class="start-all-list">
        ${Win11.startOrder.map(id => {
          const meta = Win11.appsMeta[id];
          if (!meta) return '';
          return `<button class="all-app-row" data-open-app="${id}">${Icon.app(meta.icon)}<span>${U.escapeHtml(meta.name)}</span></button>`;
        }).join('')}
      </div>`;
    panel.appendChild(all);
    const back = all.querySelector('.start-back');
    if (back) back.addEventListener('click', () => all.remove());
  }

  function showStartPower(anchor) {
    showMenu(anchor, [
      { label: '睡眠', icon: 'moon', action: sleepNow },
      { label: '关机', icon: 'power', action: () => powerAction('shutdown') },
      { label: '重启', icon: 'restart', action: () => powerAction('restart') },
      { separator: true },
      { label: '锁定', icon: 'lock', action: lockNow }
    ], 'top-end');
  }

  function showStartUser(anchor) {
    showMenu(anchor, [
      { label: '更改账户设置', icon: 'user', action: () => openSettings('accounts') },
      { label: '锁定', icon: 'lock', action: lockNow },
      { separator: true },
      { label: '注销', icon: 'power', action: lockNow }
    ], 'top-start');
  }

  function renderStart() {
    const panel = flyoutEl('start');
    if (!panel) return;
    const name = Win11.userName();
    panel.innerHTML = `
      <div class="start-search">${Icon.ui('search')}<input id="start-search-input" type="text" placeholder="搜索应用、设置和文档" aria-label="搜索" autocomplete="off" spellcheck="false"></div>
      <div class="start-scroll" id="start-scroll"></div>
      <div class="start-footer">
        <button class="start-user" id="start-user" title="账户">
          <span class="avatar">${U.escapeHtml(Win11.initialOf(name))}</span>
          <span>${U.escapeHtml(name)}</span>
        </button>
        <button class="start-power" id="start-power" title="电源" aria-label="电源">${Icon.ui('power')}</button>
      </div>`;
    paintStartBody('');
    const input = panel.querySelector('#start-search-input');
    if (input) {
      input.addEventListener('input', () => paintStartBody(input.value));
      input.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
          e.preventDefault();
          const first = panel.querySelector('[data-search-result]');
          if (first) first.click();
        }
        if (e.key === 'Escape') {
          e.preventDefault();
          closeAllFlyouts();
        }
      });
    }
    const scroll = panel.querySelector('#start-scroll');
    if (scroll) {
      scroll.addEventListener('click', e => {
        if (e.target.closest('[data-start-all]')) { showAllApps(); return; }
        if (e.target.closest('[data-start-more]')) {
          UI.openApp('explorer', { nodeId: 'desktop' });
        }
      });
    }
    const power = panel.querySelector('#start-power');
    if (power) power.addEventListener('click', e => { e.stopPropagation(); showStartPower(power); });
    const user = panel.querySelector('#start-user');
    if (user) user.addEventListener('click', e => { e.stopPropagation(); showStartUser(user); });
  }

  /* -------------------------------- search --------------------------------- */
  function appResultMarkup(app) {
    return `<button class="search-result" data-search-result data-open-app="${app.id}">
      ${Icon.app(app.meta.icon)}
      <span class="search-result-main"><span class="search-result-name">${U.escapeHtml(app.meta.name)}</span><span class="search-result-sub">应用 · ${app.id}</span></span>
      <span class="search-meta">应用</span>
    </button>`;
  }

  function fileResultMarkup(node) {
    return `<button class="search-result" data-search-result data-open-file="${U.escapeHtml(node.id)}">
      ${nodeAppIcon(node)}
      <span class="search-result-main"><span class="search-result-name">${U.escapeHtml(node.name)}</span><span class="search-result-sub">${U.escapeHtml(filePathString(node))}</span></span>
      <span class="search-meta">${U.escapeHtml(fileTypeLabel(node))}</span>
    </button>`;
  }

  function searchResultsMarkup(query) {
    const q = String(query || '').trim();
    const res = searchAll(q);
    if (!res.apps.length && !res.files.length) {
      return `<div class="search-empty">${Icon.ui('search')}<div>没有找到与「${U.escapeHtml(q)}」相关的内容</div><div class="faint" style="margin-top:6px">试试搜索应用名、文件名或文件内容标题。</div></div>`;
    }
    let html = '';
    if (res.apps.length) {
      html += `<div class="search-label">应用</div><div class="search-results">${res.apps.map(appResultMarkup).join('')}</div>`;
    }
    if (res.files.length) {
      html += `<div class="search-label">文件</div><div class="search-results">${res.files.map(fileResultMarkup).join('')}</div>`;
    }
    return html;
  }

  function paintSearchBody(query) {
    const body = $('search-body');
    if (!body) return;
    const q = String(query || '').trim();
    if (q) {
      body.innerHTML = searchResultsMarkup(q);
      return;
    }
    body.innerHTML = `
      <div class="search-label">热门应用</div>
      <div class="search-apps">
        ${Win11.startOrder.map(id => {
          const meta = Win11.appsMeta[id];
          if (!meta) return '';
          return `<button class="search-app-chip" data-open-app="${id}">${Icon.app(meta.icon)}<span>${U.escapeHtml(meta.name)}</span></button>`;
        }).join('')}
      </div>
      <div class="search-label">最近使用的文件</div>
      <div class="search-results">${recommendedFileMarkup(true)}</div>
      <div class="search-footer-note">${Icon.ui('info')}搜索只会在本地离线数据和内置应用中进行。</div>
    `;
  }

  function renderSearch() {
    const panel = flyoutEl('search');
    if (!panel) return;
    panel.innerHTML = `
      <div class="search-top">
        ${Icon.ui('search')}
        <input id="search-input" type="text" placeholder="输入以搜索应用、文件和设置" aria-label="搜索" autocomplete="off" spellcheck="false">
        <button class="search-close" id="search-close" aria-label="关闭搜索">${Icon.ui('close')}</button>
      </div>
      <div class="search-body" id="search-body"></div>`;
    paintSearchBody('');
    const input = panel.querySelector('#search-input');
    if (input) {
      input.addEventListener('input', () => paintSearchBody(input.value));
      input.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
          e.preventDefault();
          const first = panel.querySelector('[data-search-result]');
          if (first) first.click();
        }
        if (e.key === 'Escape') {
          e.preventDefault();
          closeAllFlyouts();
        }
      });
    }
    const close = panel.querySelector('#search-close');
    if (close) close.addEventListener('click', closeAllFlyouts);
  }

  /* -------------------------------- widgets -------------------------------- */
  function renderWidgets() {
    const panel = flyoutEl('widgets');
    if (!panel) return;
    const w = Win11.weather;
    const now = new Date();
    const wall = Win11.wallpaperOf(Store.state.wallpaper);
    const hourly = w.hourly.slice(0, 7).map(h => `
      <div class="ww-hour"><span>${h.t}</span>${Icon.ui(h.icon)}<strong>${h.deg}°</strong></div>`).join('');
    panel.innerHTML = `
      <div class="widgets-head">
        <div class="title">小组件</div>
        <div class="avatar">${U.escapeHtml(Win11.initialOf())}</div>
      </div>
      <div class="widgets-scroll">
        <div class="widget-card widget-weather">
          <div class="ww-top">
            <div><div class="ww-city">${Icon.ui('location')}<span>${U.escapeHtml(w.city)}</span></div><div class="ww-cond">${U.escapeHtml(w.condition)} · 体感 ${w.feels}°</div></div>
            <span class="ww-icon">${Icon.ui('sun')}</span>
          </div>
          <div class="ww-temp">${w.temp}°</div>
          <div class="ww-range">最高 ${w.high}° · 最低 ${w.low}°</div>
          <div class="ww-hours">${hourly}</div>
          <button class="ww-link" data-open-app="weather">查看完整天气</button>
        </div>
        <div class="widget-row">
          <div class="widget-card widget-mini">
            <div class="widget-mini-head">${Icon.ui('calendar')}<span>日历</span></div>
            <div class="widget-cal-date">${now.getDate()}</div>
            <div class="widget-cal-day">${WEEK[now.getDay()]} · ${MONTHS[now.getMonth()]}</div>
            <div class="widget-cal-event"><strong>16:00</strong> 项目同步 · 团队例会</div>
          </div>
          <div class="widget-card widget-mini">
            <div class="widget-mini-head">${Icon.ui('check')}<span>待办事项</span></div>
            <div class="widget-todo">
              <div class="todo-line done"><span class="todo-check done"></span><span>整理桌面图标</span></div>
              <div class="todo-line done"><span class="todo-check done"></span><span>完成窗口贴靠</span></div>
              <div class="todo-line"><span class="todo-check"></span><span>去河边看日落</span></div>
              <div class="todo-line"><span class="todo-check"></span><span>把照片设为壁纸</span></div>
            </div>
          </div>
        </div>
        <div class="widget-card widget-photos" data-open-app="photos" role="button" tabindex="0" title="打开照片">
          <img src="${wall.file}" alt="今日照片" draggable="false">
          <div class="widget-photos-meta">${Icon.ui('picture')}<span>今日照片 · ${U.escapeHtml(wall.name)}</span></div>
        </div>
        <div class="widget-card widget-stocks">
          <div class="widget-mini-head">${Icon.ui('wind')}<span>市场行情</span></div>
          <div class="stock-row"><span class="stock-name">上证指数</span><span class="stock-up">+0.62%</span></div>
          <div class="stock-row"><span class="stock-name">深证成指</span><span class="stock-up">+0.38%</span></div>
          <div class="stock-row"><span class="stock-name">恒生指数</span><span class="stock-down">-0.21%</span></div>
        </div>
      </div>`;
  }

  /* ----------------------------- quick settings ---------------------------- */
  function paintRange(input) {
    if (!input) return;
    const min = Number(input.min || 0);
    const max = Number(input.max || 100);
    const value = Number(input.value || 0);
    const pct = max === min ? 0 : ((value - min) / (max - min)) * 100;
    input.style.setProperty('--range-progress', `${pct}%`);
  }

  function quickTileMarkup(key, name, icon, active, sub) {
    return `<button class="qs-tile ${active ? 'active' : ''}" data-qs="${key}" aria-pressed="${active}">
      ${Icon.ui(icon)}
      <span class="qs-name">${U.escapeHtml(name)}</span>
      <span class="qs-sub">${U.escapeHtml(sub)}</span>
    </button>`;
  }

  function renderQuick() {
    const panel = flyoutEl('quick');
    if (!panel) return;
    const s = Store.state;
    const wifiOn = s.wifi && !s.airplane;
    const btOn = s.bluetooth && !s.airplane;
    panel.innerHTML = `
      <div class="qs-head"><strong>快速设置</strong><span>${U.escapeHtml(Win11.userName())}</span></div>
      <div class="qs-grid">
        ${quickTileMarkup('wifi', 'Wi-Fi', wifiOn ? 'wifi' : 'wifi-off', wifiOn, wifiOn ? 'Contoso Wi-Fi' : s.airplane ? '飞行模式中' : '未连接')}
        ${quickTileMarkup('bluetooth', '蓝牙', 'bluetooth', btOn, btOn ? '已连接 2 台设备' : s.airplane ? '飞行模式中' : '已关闭')}
        ${quickTileMarkup('airplane', '飞行模式', 'airplane', s.airplane, s.airplane ? '已开启' : '已关闭')}
        ${quickTileMarkup('saver', '节能模式', 'leaf', s.saver, s.saver ? '已开启' : '平衡模式')}
        ${quickTileMarkup('nightLight', '夜间模式', 'moon', s.nightLight, s.nightLight ? '已开启' : '已关闭')}
        ${quickTileMarkup('accessibility', '辅助功能', 'access', accessibilityOn, accessibilityOn ? '已开启' : '已关闭')}
      </div>
      <div class="qs-sliders">
        <div class="qs-slider">${Icon.ui('sun')}<input type="range" id="qs-brightness" min="25" max="100" step="1" value="${U.clamp(Number(s.brightness), 25, 100)}" aria-label="亮度"><span class="qs-value" id="qs-brightness-val">${s.brightness}%</span></div>
        <div class="qs-slider">${Icon.ui(Number(s.volume) <= 0 ? 'volume-mute' : 'volume')}<input type="range" id="qs-volume" min="0" max="100" step="1" value="${U.clamp(Number(s.volume), 0, 100)}" aria-label="音量"><span class="qs-value" id="qs-volume-val">${s.volume}%</span></div>
      </div>
      <div class="qs-footer">
        <div class="qs-battery">${Icon.ui('battery')}<div><div style="font-size:11.5px">78%</div><small>剩余约 4 小时 12 分钟</small></div></div>
        <button class="icon-btn" data-open-settings="system" title="所有设置" aria-label="所有设置">${Icon.ui('settings')}</button>
      </div>`;
    const brightness = panel.querySelector('#qs-brightness');
    const volume = panel.querySelector('#qs-volume');
    if (brightness) {
      paintRange(brightness);
      brightness.addEventListener('input', () => {
        const value = Number(brightness.value);
        Store.set('brightness', value);
        paintRange(brightness);
        const label = panel.querySelector('#qs-brightness-val');
        if (label) label.textContent = `${value}%`;
      });
    }
    if (volume) {
      paintRange(volume);
      volume.addEventListener('input', () => {
        const value = Number(volume.value);
        Store.set('volume', value);
        paintRange(volume);
        const label = panel.querySelector('#qs-volume-val');
        if (label) label.textContent = `${value}%`;
      });
    }
    panel.querySelectorAll('[data-qs]').forEach(btn => {
      btn.addEventListener('click', () => toggleQuick(btn.dataset.qs));
    });
  }

  function toggleQuick(key) {
    const s = Store.state;
    if (key === 'accessibility') {
      accessibilityOn = !accessibilityOn;
      toast('辅助功能', accessibilityOn ? '辅助功能选项已开启。' : '辅助功能选项已关闭。', { icon: Icon.ui('access') });
      renderQuick();
      return;
    }
    if (key === 'airplane') {
      const next = !s.airplane;
      Store.patch({ airplane: next, ...(next ? { wifi: false, bluetooth: false } : {}) });
      toast(next ? '飞行模式已开启' : '飞行模式已关闭', next ? '所有无线通信已暂停。' : '可以重新连接 Wi-Fi 和蓝牙设备。', { icon: Icon.ui('airplane') });
      renderQuick();
      return;
    }
    if (key === 'wifi') {
      const next = !s.wifi;
      Store.patch({ wifi: next, ...(next ? { airplane: false } : {}) });
      if (next) toast('已连接到 Contoso Wi-Fi', '网络连接正常，可以离线浏览这个桌面。', { icon: Icon.ui('wifi') });
      renderQuick();
      return;
    }
    if (key === 'bluetooth') {
      const next = !s.bluetooth;
      Store.patch({ bluetooth: next, ...(next ? { airplane: false } : {}) });
      if (next) toast('蓝牙已开启', '现在可以发现附近的设备。', { icon: Icon.ui('bluetooth') });
      renderQuick();
      return;
    }
    Store.set(key, !s[key]);
    renderQuick();
  }

  /* --------------------------- notifications/cal --------------------------- */
  function notifIconMarkup(icon) {
    if (icon && icon.type === 'app') return Icon.app(icon.name);
    if (icon && icon.type === 'ui') return Icon.ui(icon.name);
    return Icon.ui('info');
  }

  function calendarEventFor(date) {
    const now = new Date();
    if (date.getMonth() !== now.getMonth() || date.getFullYear() !== now.getFullYear()) return '';
    return {
      5: '项目同步 · 16:00',
      12: '整理周会纪要',
      18: '提交设计稿',
      25: '给朋友过生日'
    }[date.getDate()] || '';
  }

  function sameDay(a, b) {
    return a && b && a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  function calendarMarkup() {
    const today = new Date();
    if (!calCursor) calCursor = new Date(today.getFullYear(), today.getMonth(), 1);
    if (!calSelected) calSelected = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const y = calCursor.getFullYear();
    const m = calCursor.getMonth();
    const first = new Date(y, m, 1);
    const offset = (first.getDay() + 6) % 7;
    const start = new Date(y, m, 1 - offset);
    const weekdays = ['一', '二', '三', '四', '五', '六', '日'];
    let cells = '';
    for (let i = 0; i < 42; i += 1) {
      const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      const other = date.getMonth() !== m;
      const isToday = sameDay(date, today);
      const isSelected = sameDay(date, calSelected);
      const event = calendarEventFor(date);
      cells += `<button class="cal-day ${other ? 'other' : ''} ${isToday ? 'today' : ''} ${isSelected ? 'selected' : ''}"
        data-cal-day="${date.getTime()}" aria-label="${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}">${date.getDate()}${event ? '<span class="cal-dot"></span>' : ''}</button>`;
    }
    const note = calendarEventFor(calSelected);
    return `
      <div class="cal-card">
        <div class="cal-head">
          <div class="cal-month">${y}年${m + 1}月</div>
          <div class="cal-nav">
            <button data-cal-nav="-1" aria-label="上个月">${Icon.ui('chevron-left')}</button>
            <button data-cal-nav="1" aria-label="下个月">${Icon.ui('chevron-right')}</button>
          </div>
        </div>
        <div class="cal-weekdays">${weekdays.map(day => `<span>${day}</span>`).join('')}</div>
        <div class="cal-days">${cells}</div>
        <div class="cal-note"><strong>${calSelected.getMonth() + 1}月${calSelected.getDate()}日</strong> · ${note || '今天没有日程安排'}</div>
      </div>`;
  }

  function renderNotifications() {
    const panel = flyoutEl('notif');
    if (!panel) return;
    const items = Win11.notifications;
    const list = items.length ? items.map(item => `
      <div class="notif-card" data-notif-id="${U.escapeHtml(item.id)}">
        <div class="notif-icon">${notifIconMarkup(item.icon)}</div>
        <div class="notif-main">
          <div class="notif-app">${U.escapeHtml(item.appName)}<span>·</span><span>${U.escapeHtml(item.time)}</span></div>
          <div class="notif-title">${U.escapeHtml(item.title)}</div>
          <div class="notif-body">${U.escapeHtml(item.body)}</div>
        </div>
        <button class="notif-close" data-notif-close="${U.escapeHtml(item.id)}" aria-label="清除通知">${Icon.ui('close')}</button>
      </div>`).join('') : `<div class="np-empty">${Icon.ui('bell-off')}<div>没有新通知</div></div>`;
    panel.innerHTML = `
      <div class="np-head"><strong>通知</strong>${items.length ? '<button id="np-clear">清除全部</button>' : ''}</div>
      <div class="np-scroll">
        <div class="np-list">${list}</div>
        ${calendarMarkup()}
      </div>`;
    panel.querySelectorAll('[data-notif-close]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.dataset.notifClose;
        const index = Win11.notifications.findIndex(item => item.id === id);
        if (index >= 0) Win11.notifications.splice(index, 1);
        renderNotifications();
      });
    });
    const clear = panel.querySelector('#np-clear');
    if (clear) {
      clear.addEventListener('click', () => {
        Win11.notifications.length = 0;
        renderNotifications();
      });
    }
    panel.querySelectorAll('[data-cal-nav]').forEach(btn => {
      btn.addEventListener('click', () => {
        calCursor = new Date(calCursor.getFullYear(), calCursor.getMonth() + Number(btn.dataset.calNav), 1);
        renderNotifications();
      });
    });
    panel.querySelectorAll('[data-cal-day]').forEach(btn => {
      btn.addEventListener('click', () => {
        const date = new Date(Number(btn.dataset.calDay));
        calSelected = date;
        if (date.getMonth() !== calCursor.getMonth() || date.getFullYear() !== calCursor.getFullYear()) {
          calCursor = new Date(date.getFullYear(), date.getMonth(), 1);
        }
        renderNotifications();
      });
    });
    updateNotifDot();
  }

  /* ------------------------------- task view ------------------------------- */
  function renderTaskview() {
    const inner = $('taskview-inner');
    if (!inner) return;
    const wins = Win11.WM.windows.filter(w => !w.closed).sort((a, b) => (b.z || 0) - (a.z || 0));
    const cards = wins.length ? wins.map(w => {
      const meta = Win11.appsMeta[w.appId] || { name: w.title, icon: 'win' };
      return `<div class="tv-card" data-tv-window="${w.id}">
        <div class="tv-card-head">
          ${Icon.app(meta.icon)}
          <span class="tv-card-title">${U.escapeHtml(w.title || meta.name)}</span>
          <button class="tv-close" data-tv-close="${w.id}" aria-label="关闭窗口">${Icon.ui('close')}</button>
        </div>
        <div class="tv-preview apps-${w.appId}">${Icon.app(meta.icon)}</div>
      </div>`;
    }).join('') : `<div class="taskview-empty">${Icon.ui('taskview')}<div>没有打开的窗口，双击桌面图标开始吧。</div></div>`;
    inner.innerHTML = `
      <div class="taskview-head"><strong>任务视图</strong><span class="tv-hint">${wins.length ? `${wins.length} 个打开的窗口` : '窗口会显示在这里'}</span></div>
      <div class="taskview-grid">${cards}</div>
      <div class="taskview-desktops">
        <button class="tvd-chip active">${Icon.ui('desktop')}桌面 1</button>
        <button class="tvd-chip" data-new-desktop>${Icon.ui('plus')}新建桌面</button>
      </div>`;
  }

  function openTaskview() {
    closeAllFlyouts();
    closeMenu();
    taskviewOpen = true;
    const tv = $('taskview');
    if (tv) tv.hidden = false;
    const btn = $('tb-taskview');
    if (btn) btn.classList.add('open');
    renderTaskview();
  }

  function closeTaskview() {
    taskviewOpen = false;
    const tv = $('taskview');
    if (tv) tv.hidden = true;
    const btn = $('tb-taskview');
    if (btn) btn.classList.remove('open');
  }

  function toggleTaskview() {
    if (taskviewOpen) closeTaskview(); else openTaskview();
  }

  function setupTaskview() {
    const inner = $('taskview-inner');
    if (inner) {
      inner.addEventListener('click', e => {
        const closeBtn = e.target.closest('[data-tv-close]');
        if (closeBtn) {
          e.stopPropagation();
          const win = Win11.WM.getWindow(closeBtn.dataset.tvClose);
          if (win) Win11.WM.close(win);
          return;
        }
        const card = e.target.closest('[data-tv-window]');
        if (card) {
          const win = Win11.WM.getWindow(card.dataset.tvWindow);
          closeTaskview();
          if (win) Win11.WM.focus(win);
          return;
        }
        if (e.target.closest('[data-new-desktop]')) {
          toast('新建桌面', '这个离线演示只使用一个虚拟桌面。', { icon: Icon.ui('desktop') });
        }
      });
    }
    const tv = $('taskview');
    if (tv) {
      tv.addEventListener('click', e => {
        if (e.target === tv) closeTaskview();
      });
    }
  }

  /* ------------------------------ lock screen ------------------------------ */
  function showSignin() {
    if (!locked) return;
    const lock = $('lock-screen');
    if (!lock) return;
    closeLockMenu();
    refreshIdentity();
    lock.classList.add('signin-visible');
    setTimeout(() => {
      const input = $('signin-password');
      if (input) { input.value = ''; input.focus(); }
    }, 130);
  }

  function hideSignin() {
    const lock = $('lock-screen');
    if (lock) lock.classList.remove('signin-visible');
    const input = $('signin-password');
    if (input) input.blur();
  }

  function lockNow() {
    closeAllFlyouts();
    closeMenu();
    closeModal();
    closeTaskview();
    closeLockMenu();
    const lock = $('lock-screen');
    if (lock) {
      lock.classList.add('is-locked');
      lock.classList.remove('signin-visible');
    }
    locked = true;
    const input = $('signin-password');
    if (input) input.value = '';
    if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
    updateClock();
    refreshIdentity();
  }

  function unlockNow() {
    const lock = $('lock-screen');
    if (lock) lock.classList.remove('is-locked', 'signin-visible');
    locked = false;
    const input = $('signin-password');
    if (input) { input.value = ''; input.blur(); }
    refreshIdentity();
  }

  function closeLockMenu() {
    if (lockMenu) {
      lockMenu.remove();
      lockMenu = null;
    }
  }

  function showLockPowerMenu(anchor) {
    closeLockMenu();
    const menu = document.createElement('div');
    menu.className = 'lock-power-menu';
    const items = [
      { act: 'sleep', label: '睡眠', icon: 'moon' },
      { act: 'shutdown', label: '关机', icon: 'power' },
      { act: 'restart', label: '重启', icon: 'restart' }
    ];
    menu.innerHTML = items.map(item => `
      <button class="ctx-item" data-lock-action="${item.act}">
        <span class="ctx-icon-space">${Icon.ui(item.icon)}</span>
        <span class="ctx-label">${item.label}</span>
      </button>`).join('');
    document.body.appendChild(menu);
    menu.style.visibility = 'hidden';
    const r = anchor.getBoundingClientRect();
    const w = menu.offsetWidth;
    const h = menu.offsetHeight;
    let x = Math.max(10, Math.min(r.right - w, window.innerWidth - w - 10));
    let y = r.top - h - 10;
    if (y < 10) y = Math.min(window.innerHeight - h - 10, r.bottom + 10);
    menu.style.left = `${x}px`;
    menu.style.top = `${y}px`;
    menu.style.visibility = '';
    lockMenu = menu;
    menu.addEventListener('click', e => {
      const btn = e.target.closest('[data-lock-action]');
      if (!btn) return;
      const action = btn.dataset.lockAction;
      closeLockMenu();
      if (action === 'sleep') sleepNow();
      if (action === 'shutdown') powerAction('shutdown');
      if (action === 'restart') powerAction('restart');
    });
  }

  function sleepNow() {
    lockNow();
    const overlay = document.createElement('div');
    overlay.className = 'sleep-screen';
    const now = new Date();
    overlay.innerHTML = `<div class="sleep-content">
      <div class="sleep-time">${pad(now.getHours())}:${pad(now.getMinutes())}</div>
      <div class="sleep-hint">点击任意位置唤醒</div>
    </div>`;
    document.body.appendChild(overlay);
    let awake = false;
    const wake = () => {
      if (awake) return;
      awake = true;
      document.removeEventListener('keydown', onKey, true);
      overlay.classList.add('leaving');
      setTimeout(() => {
        overlay.remove();
        showSignin();
      }, 300);
    };
    const onKey = e => {
      e.preventDefault();
      e.stopPropagation();
      wake();
    };
    overlay.addEventListener('click', wake);
    document.addEventListener('keydown', onKey, true);
  }

  function closeAllWindows() {
    Win11.WM.windows.filter(w => !w.closed).forEach(w => {
      try { Win11.WM.close(w); } catch (e) { /* already gone */ }
    });
  }

  function powerAction(kind) {
    closeAllFlyouts();
    closeMenu();
    closeModal();
    closeTaskview();
    closeLockMenu();
    closeAllWindows();
    const boot = $('boot-screen');
    const text = $('boot-text');
    const shutdown = $('shutdown-screen');
    if (shutdown) shutdown.hidden = true;
    if (text) text.textContent = kind === 'shutdown' ? '正在关机…' : '正在重启…';
    if (boot) boot.hidden = false;
    setTimeout(() => {
      if (boot) boot.hidden = true;
      if (kind === 'shutdown') {
        if (shutdown) shutdown.hidden = false;
      } else {
        lockNow();
      }
    }, kind === 'shutdown' ? 2000 : 2200);
  }

  function startFromShutdown() {
    const boot = $('boot-screen');
    const text = $('boot-text');
    const shutdown = $('shutdown-screen');
    if (shutdown) shutdown.hidden = true;
    if (text) text.textContent = '正在启动…';
    if (boot) boot.hidden = false;
    setTimeout(() => {
      if (boot) boot.hidden = true;
      lockNow();
    }, 1800);
  }

  function setupLock() {
    const form = $('signin-form');
    if (form) {
      form.addEventListener('submit', e => {
        e.preventDefault();
        unlockNow();
      });
    }
    const lock = $('lock-screen');
    if (lock) {
      lock.addEventListener('click', e => {
        if (!locked) return;
        if (e.target.closest('.lock-btn')) return;
        if (lock.classList.contains('signin-visible')) return;
        showSignin();
      });
    }
    const power = $('lock-power');
    if (power) {
      power.addEventListener('click', e => {
        e.stopPropagation();
        if (lockMenu) closeLockMenu(); else showLockPowerMenu(power);
      });
    }
    const shutdownPower = $('shutdown-power');
    if (shutdownPower) shutdownPower.addEventListener('click', startFromShutdown);
    document.addEventListener('pointerdown', e => {
      if (lockMenu && !e.target.closest('.lock-power-menu') && !e.target.closest('#lock-power')) closeLockMenu();
    }, true);
    window.addEventListener('resize', closeLockMenu);
  }

  /* ------------------------------- shortcuts ------------------------------- */
  function isEditableTarget(target) {
    return !!(target && target.closest && target.closest('input, textarea, select, [contenteditable="true"]'));
  }

  function onGlobalKeydown(e) {
    if (lockMenu && e.key === 'Escape') {
      e.preventDefault();
      closeLockMenu();
      return;
    }
    const boot = $('boot-screen');
    const shutdown = $('shutdown-screen');
    if ((boot && !boot.hidden) || (shutdown && !shutdown.hidden)) return;

    if (locked) {
      const lock = $('lock-screen');
      if (lock && lock.classList.contains('signin-visible')) {
        if (e.key === 'Escape') {
          e.preventDefault();
          hideSignin();
        }
        return;
      }
      if (['Shift', 'Control', 'Alt', 'Meta', 'CapsLock', 'Tab', 'Escape'].includes(e.key)) return;
      e.preventDefault();
      showSignin();
      return;
    }

    if (e.key === 'Escape') {
      if (currentModal) { closeModal(); return; }
      if (currentMenu) { closeMenu(); return; }
      if (taskviewOpen) { closeTaskview(); return; }
      if (openPanel) { closeAllFlyouts(); return; }
      return;
    }

    const mod = e.ctrlKey || e.metaKey;
    if (mod && !e.altKey && e.key.toLowerCase() === 'k' && !isEditableTarget(e.target)) {
      e.preventDefault();
      openSearchPanel();
      return;
    }
    if (mod && e.altKey && e.key.toLowerCase() === 't') {
      e.preventDefault();
      UI.openApp('terminal');
      return;
    }
    if (mod && e.altKey && e.key.toLowerCase() === 'l') {
      e.preventDefault();
      lockNow();
      return;
    }
    if (mod && !e.altKey && e.key === ',') {
      e.preventDefault();
      openSettings('system');
    }
  }

  function setupShortcuts() {
    document.addEventListener('keydown', onGlobalKeydown);
  }

  function setupGlobalClicks() {
    document.addEventListener('click', e => {
      const appBtn = e.target.closest('[data-open-app]');
      if (appBtn) {
        e.preventDefault();
        UI.openApp(appBtn.dataset.openApp);
        return;
      }
      const fileBtn = e.target.closest('[data-open-file]');
      if (fileBtn) {
        e.preventDefault();
        const node = fs.find(fileBtn.dataset.openFile);
        closeAllFlyouts();
        closeTaskview();
        if (node) Win11.openFsNode(node);
        return;
      }
      const settingsBtn = e.target.closest('[data-open-settings]');
      if (settingsBtn) {
        e.preventDefault();
        openSettings(settingsBtn.dataset.openSettings);
      }
    });
  }

  function setupWindowEvents() {
    window.addEventListener('win11:windows', () => {
      renderTaskbar();
      if (taskviewOpen) renderTaskview();
    });
  }

  function setupContextMenuGuard() {
    document.addEventListener('contextmenu', e => {
      if (isEditableTarget(e.target)) return;
      if (e.target.closest('.desktop, .taskbar, .flyout, .window, .lock-screen')) e.preventDefault();
    });
  }

  /* ---------------------------------- init --------------------------------- */
  Win11.refreshDesktop = refreshDesktop;
  UI.lock = lockNow;
  UI.unlock = unlockNow;
  UI.power = powerAction;
  UI.openSettings = openSettings;

  UI.init = function () {
    hydrateStaticIcons();
    initWallpaper();
    applyOverlays();
    refreshIdentity();
    renderTaskbar();
    renderDesktop();
    renderTrayIcons();
    updateClock();
    if (clockTimer) clearInterval(clockTimer);
    clockTimer = setInterval(updateClock, 1000);
    setupTaskbar();
    setupDesktop();
    setupFlyoutTriggers();
    setupTaskview();
    setupLock();
    setupShortcuts();
    setupGlobalClicks();
    setupWindowEvents();
    setupContextMenuGuard();
    updateNotifDot();
  };
})();
