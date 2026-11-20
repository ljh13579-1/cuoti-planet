/* Application renderers. */
(function () {
  const Win11 = window.Win11;
  const U = Win11.util;
  const fs = Win11.fs;
  const Icon = Win11.Icon;
  const apps = {};

  const folderIcon = node => {
    if (node.type === 'drive') return Icon.app('drive');
    if (node.type === 'folder') return Icon.app('folder');
    if (node.kind === 'image') return Icon.app('imagefile');
    return Icon.app('textfile');
  };

  const parentLabel = node => {
    const p = fs.parentOf(node);
    return p ? p.name : '此电脑';
  };

  const fileTypeText = node => {
    if (node.type === 'folder') return '文件夹';
    if (node.type === 'drive') return '磁盘驱动器';
    if (node.kind === 'image') return 'SVG 图像';
    if (node.kind === 'text') return (node.name.split('.').pop() || 'TXT').toUpperCase() + ' 文件';
    return '文件';
  };

  function openNode(node) {
    if (!node) return;
    if (node.type === 'folder' || node.type === 'drive') {
      Win11.WM.open('explorer', { nodeId: node.id });
    } else if (node.kind === 'text') {
      Win11.WM.open('notepad', { fileId: node.id });
    } else if (node.kind === 'image') {
      Win11.WM.open('photos', { fileId: node.id });
    }
  }
  Win11.openFsNode = openNode;

  function refreshDesktop() {
    if (typeof Win11.refreshDesktop === 'function') Win11.refreshDesktop();
  }

  function moveToRecycle(node) {
    const parent = fs.parentOf(node);
    if (!parent) {
      Win11.ui.toast('无法删除', '这个位置受保护，不能删除。');
      return false;
    }
    fs.remove(node);
    Win11.recycle.unshift({ node, parentId: parent.id, parentName: parent.name, at: new Date() });
    refreshDesktop();
    if (typeof Win11.refreshRecycleWindows === 'function') Win11.refreshRecycleWindows();
    Win11.ui.toast('已移到回收站', `${node.name} 已移到回收站。`, { icon: Icon.app('recycle') });
    return true;
  }

  function nodeIsWallpaper(node) {
    return Win11.wallpapers.find(w => w.file === node.image) || null;
  }

  function uniqueName(base, parent, ext) {
    const names = new Set((parent.children || []).map(c => c.name));
    let name = base + (ext || '');
    let i = 2;
    while (names.has(name)) {
      name = `${base} (${i})${ext || ''}`;
      i += 1;
    }
    return name;
  }

  /* ============================= File Explorer ============================= */
  apps.explorer = function (win, opts) {
    let current = opts && opts.nodeId ? fs.find(opts.nodeId) : null;
    let viewMode = 'grid';
    let sortBy = 'name';
    let query = '';
    let selectedId = null;
    const history = [current ? current.id : null];
    let hi = 0;

    win.body.innerHTML = `
      <div class="ex">
        <div class="ex-toolbar">
          <div class="ex-nav">
            <button class="icon-btn" data-nav="back" title="后退">${Icon.ui('arrow-left')}</button>
            <button class="icon-btn" data-nav="forward" title="前进">${Icon.ui('chevron-right')}</button>
            <button class="icon-btn" data-nav="up" title="向上">${Icon.ui('up')}</button>
            <button class="icon-btn" data-nav="refresh" title="刷新">${Icon.ui('refresh')}</button>
          </div>
          <div class="ex-address" id="ex-address"></div>
          <div class="ex-search">
            ${Icon.ui('search')}
            <input type="text" id="ex-search-input" placeholder="搜索" aria-label="搜索当前文件夹">
          </div>
        </div>
        <div class="ex-command">
          <button class="ex-cmd" data-cmd="new">${Icon.ui('plus')}<span>新建</span>${Icon.ui('chevron-down')}</button>
          <span class="ex-cmd-divider"></span>
          <button class="ex-cmd" data-cmd="sort">${Icon.ui('sort')}<span>排序</span></button>
          <button class="ex-cmd" data-cmd="view">${Icon.ui(viewMode === 'grid' ? 'grid' : 'list')}<span>查看</span></button>
          <span class="ex-right">
            <button class="ex-cmd" data-cmd="refresh">${Icon.ui('refresh')}<span>刷新</span></button>
          </span>
        </div>
        <div class="ex-main">
          <aside class="ex-sidebar" id="ex-sidebar"></aside>
          <div class="ex-content" id="ex-content" tabindex="0"></div>
        </div>
        <div class="ex-status">
          <span id="ex-count">0 个项目</span>
          <span class="grow"></span>
          <span id="ex-selected"></span>
        </div>
      </div>
    `;

    const rootEl = win.body.firstElementChild;
    const content = rootEl.querySelector('#ex-content');
    const address = rootEl.querySelector('#ex-address');
    const sidebar = rootEl.querySelector('#ex-sidebar');
    const countEl = rootEl.querySelector('#ex-count');
    const selectedEl = rootEl.querySelector('#ex-selected');
    const searchInput = rootEl.querySelector('#ex-search-input');
    const backBtn = rootEl.querySelector('[data-nav="back"]');
    const forwardBtn = rootEl.querySelector('[data-nav="forward"]');
    const upBtn = rootEl.querySelector('[data-nav="up"]');
    const viewBtn = rootEl.querySelector('[data-cmd="view"]');

    const quickItems = [
      { id: null, label: '主页', icon: 'home' },
      { id: 'desktop', label: '桌面', icon: 'desktop', fs: true },
      { id: 'downloads', label: '下载', icon: 'download', fs: true },
      { id: 'docs', label: '文档', icon: 'doc', fs: true },
      { id: 'pictures', label: '图片', icon: 'picture', fs: true },
      { id: 'music', label: '音乐', icon: 'music', fs: true },
      { id: 'videos', label: '视频', icon: 'video', fs: true }
    ];

    function navStackCurrent() { return history[hi] ? fs.find(history[hi]) : null; }

    function navigate(node, push) {
      if (push !== false) {
        if (history[hi] !== (node ? node.id : null)) {
          history.splice(hi + 1);
          history.push(node ? node.id : null);
          hi = history.length - 1;
        }
      }
      current = node || null;
      query = '';
      searchInput.value = '';
      selectedId = null;
      renderAll();
    }

    function goBack() {
      if (hi <= 0) return;
      hi -= 1;
      current = history[hi] ? fs.find(history[hi]) : null;
      query = '';
      searchInput.value = '';
      selectedId = null;
      renderAll();
    }
    function goForward() {
      if (hi >= history.length - 1) return;
      hi += 1;
      current = history[hi] ? fs.find(history[hi]) : null;
      query = '';
      searchInput.value = '';
      selectedId = null;
      renderAll();
    }
    function goUp() {
      if (!current) return;
      const parent = fs.parentOf(current);
      if (parent) navigate(parent);
      else navigate(null);
    }

    function sortedChildren(node) {
      let items = (node && node.children) ? node.children.slice() : [];
      if (query.trim()) {
        const q = query.trim().toLowerCase();
        items = items.filter(n => n.name.toLowerCase().includes(q));
      }
      items.sort((a, b) => {
        const af = a.type === 'folder' || a.type === 'drive';
        const bf = b.type === 'folder' || b.type === 'drive';
        if (af !== bf) return af ? -1 : 1;
        if (sortBy === 'date') return String(b.modified).localeCompare(String(a.modified));
        if (sortBy === 'type') return (a.kind || a.type).localeCompare(b.kind || b.type) || a.name.localeCompare(b.name, 'zh-CN');
        return a.name.localeCompare(b.name, 'zh-CN');
      });
      return items;
    }

    function renderSidebar() {
      const activeId = current ? current.id : null;
      let html = '';
      quickItems.forEach(item => {
        const node = item.fs ? fs.find(item.id) : null;
        if (item.fs && !node) return;
        const on = activeId !== null ? activeId === item.id : item.id === null;
        html += `<button class="ex-side-item ${on ? 'active' : ''}" data-side="${item.id === null ? 'home' : item.id}">
          ${Icon.ui(item.icon)}<span>${U.escapeHtml(item.label)}</span></button>`;
      });
      html += `<div class="ex-side-label">此电脑</div>`;
      const c = fs.find('c-drive');
      if (c) {
        html += `<button class="ex-side-item ${activeId === 'c-drive' ? 'active' : ''}" data-side="c-drive">
          ${Icon.app('drive')}<span>${U.escapeHtml(c.name)}</span></button>`;
      }
      html += `<button class="ex-side-item ${activeId === 'thispc' ? 'active' : ''}" data-side="thispc">
        ${Icon.ui('pc')}<span>此电脑</span></button>`;
      html += `<div class="ex-storage">
        <div>本地磁盘 (C:)</div>
        <div class="ex-storage-bar"><i></i></div>
        <div>已使用 148 GB，共 256 GB</div>
      </div>`;
      sidebar.innerHTML = html;
      sidebar.querySelectorAll('[data-side]').forEach(btn => {
        btn.addEventListener('click', () => {
          const id = btn.dataset.side;
          navigate(id === 'home' ? null : fs.find(id));
        });
      });
    }

    function renderAddress() {
      let html = '';
      if (!current) {
        html = `<button class="ex-crumb current">${Icon.ui('home')}<span>主页</span></button>`;
      } else {
        const path = fs.pathOf(current);
        path.forEach((n, i) => {
          const last = i === path.length - 1;
          html += `<button class="ex-crumb ${last ? 'current' : ''}" data-crumb="${n.id}">${U.escapeHtml(n.name)}</button>`;
          if (!last) html += `<span class="ex-sep">${Icon.ui('chevron-right')}</span>`;
        });
      }
      address.innerHTML = html;
      address.querySelectorAll('[data-crumb]').forEach(b => {
        b.addEventListener('click', () => navigate(fs.find(b.dataset.crumb)));
      });
      backBtn.disabled = hi <= 0;
      forwardBtn.disabled = hi >= history.length - 1;
      upBtn.disabled = !current;
    }

    function renderHome() {
      const folders = ['desktop', 'downloads', 'docs', 'pictures'].map(id => fs.find(id)).filter(Boolean);
      const folderCards = folders.map(n => `
        <button class="quick-folder" data-open="${n.id}">
          ${Icon.app('folder')}
          <span class="quick-folder-main">
            <span class="quick-folder-name">${U.escapeHtml(n.name)}</span>
            <span class="quick-folder-sub">${(n.children || []).length} 个项目</span>
          </span>
        </button>`).join('');
      const recentIds = ['welcome', 'plan', 'meeting', 'pic-bloom', 'travel'];
      const recent = recentIds.map(id => fs.find(id)).filter(Boolean).map(n => `
        <button class="recent-row" data-open="${n.id}">
          ${folderIcon(n)}
          <span class="recent-name">${U.escapeHtml(n.name)}</span>
          <span class="recent-path">${U.escapeHtml(parentLabel(n))}</span>
          <span class="recent-date">${U.escapeHtml(String(n.modified || ''))}</span>
        </button>`).join('');
      return `<div class="ex-home">
        <h2 class="ex-home-title">主页</h2>
        <div class="quick-folders">${folderCards}</div>
        <div class="ex-file-block-title">最近使用的文件 <span class="faint">来自此电脑</span></div>
        <div class="recent-files">${recent}</div>
      </div>`;
    }

    function renderContent() {
      if (!current) {
        content.innerHTML = renderHome();
        content.querySelectorAll('[data-open]').forEach(btn => {
          btn.addEventListener('dblclick', () => openNode(fs.find(btn.dataset.open)));
          btn.addEventListener('click', e => {
            if (e.detail === 1) {
              content.querySelectorAll('.selected').forEach(x => x.classList.remove('selected'));
              btn.classList.add('selected');
            }
          });
        });
        countEl.textContent = '快速访问';
        selectedEl.textContent = '';
        return;
      }

      const items = sortedChildren(current);
      if (!items.length) {
        content.innerHTML = `<div class="ex-empty">
          ${query ? Icon.ui('search') : Icon.app('folder')}
          <div>${query ? '没有找到匹配的项目。' : '此文件夹为空'}</div>
          <div class="tiny-meta">${query ? '试试其他关键词，或清除搜索。' : '可以在命令栏里新建一个文件夹或文本文档。'}</div>
        </div>`;
        countEl.textContent = '0 个项目';
        selectedEl.textContent = '';
        return;
      }

      if (viewMode === 'grid') {
        content.innerHTML = `<div class="ex-grid">${items.map(n => `
          <div class="ex-tile ${selectedId === n.id ? 'selected' : ''}" data-file="${n.id}" tabindex="-1">
            ${folderIcon(n)}
            <div class="ex-tile-name">${U.escapeHtml(n.name)}</div>
          </div>`).join('')}</div>`;
      } else {
        content.innerHTML = `<div class="ex-list">
          <div class="ex-list-head"><span>名称</span><span>修改日期</span><span>类型</span><span>大小</span></div>
          ${items.map(n => `
            <div class="ex-row ${selectedId === n.id ? 'selected' : ''}" data-file="${n.id}" tabindex="-1">
              <span class="ex-row-name">${folderIcon(n)}<span>${U.escapeHtml(n.name)}</span></span>
              <span class="ex-col">${U.escapeHtml(String(n.modified || '—'))}</span>
              <span class="ex-col">${U.escapeHtml(fileTypeText(n))}</span>
              <span class="ex-col">${n.type === 'folder' ? '—' : U.escapeHtml(U.formatSize(n.size))}</span>
            </div>`).join('')}
        </div>`;
      }

      let lastClick = 0;
      content.querySelectorAll('[data-file]').forEach(el => {
        const node = fs.find(el.dataset.file);
        el.addEventListener('click', () => {
          selectedId = node.id;
          content.querySelectorAll('.selected').forEach(x => x.classList.remove('selected'));
          el.classList.add('selected');
          selectedEl.textContent = node.name;
        });
        el.addEventListener('dblclick', () => openNode(node));
        el.addEventListener('contextmenu', e => {
          e.preventDefault();
          selectedId = node.id;
          content.querySelectorAll('.selected').forEach(x => x.classList.remove('selected'));
          el.classList.add('selected');
          showFileMenu(e.clientX, e.clientY, node);
        });
      });

      countEl.textContent = `${items.length} 个项目`;
      selectedEl.textContent = selectedId ? (fs.find(selectedId) || {}).name || '' : '';
    }

    function renderAll() {
      renderSidebar();
      renderAddress();
      renderContent();
      viewBtn.innerHTML = `${Icon.ui(viewMode === 'grid' ? 'grid' : 'list')}<span>查看</span>`;
      win.setTitle(current ? `${current.name} - 文件资源管理器` : '主页 - 文件资源管理器');
    }

    function showFileMenu(x, y, node) {
      if (!Win11.ui || !Win11.ui.contextMenu) return;
      const items = [{ label: '打开', icon: 'folder', action: () => openNode(node) }];
      if (node.type === 'folder') items.push({ label: '在新窗口中打开', icon: 'copy', action: () => Win11.WM.open('explorer', { nodeId: node.id }) });
      if (node.kind === 'text') items.push({ label: '在记事本中打开', icon: 'edit', action: () => Win11.WM.open('notepad', { fileId: node.id }) });
      if (node.kind === 'image') items.push({ label: '在照片中打开', icon: 'picture', action: () => Win11.WM.open('photos', { fileId: node.id }) });
      const wall = nodeIsWallpaper(node);
      if (wall) items.push({ label: '设为桌面背景', icon: 'display', action: () => { Win11.Store.set('wallpaper', wall.id); Win11.ui.toast('桌面背景已更新', `现在使用「${wall.name}」。`, { icon: Icon.app('photos') }); } });
      items.push({ separator: true });
      items.push({
        label: '属性', icon: 'info', action: () => Win11.ui.toast(node.name, `${fileTypeText(node)} · ${node.type === 'folder' ? `${(node.children || []).length} 个项目` : U.formatSize(node.size)} · 修改于 ${node.modified}`)
      });
      items.push({ label: '删除', icon: 'trash', danger: true, action: () => { if (moveToRecycle(node)) { selectedId = null; renderAll(); } } });
      Win11.ui.contextMenu(x, y, items);
    }

    function newItem(kind) {
      if (!current) {
        Win11.ui.toast('无法在此处新建', '请先打开一个文件夹。');
        return;
      }
      if (current.type === 'drive' || current.type === 'folder') {
        let node;
        if (kind === 'folder') node = fs.createFolder(uniqueName('新建文件夹', current), current);
        else {
          const base = uniqueName('新建文本文档', current, '.txt');
          node = fs.createText(base, '', current);
        }
        refreshDesktop();
        if (typeof Win11.refreshRecycleWindows === 'function') {}
        renderAll();
        Win11.ui.toast(kind === 'folder' ? '已新建文件夹' : '已新建文本文档', node.name, { icon: folderIcon(node) });
      }
    }

    rootEl.querySelectorAll('[data-nav]').forEach(btn => {
      btn.addEventListener('click', () => {
        const nav = btn.dataset.nav;
        if (nav === 'back') goBack();
        if (nav === 'forward') goForward();
        if (nav === 'up') goUp();
        if (nav === 'refresh') {
          selectedId = null;
          renderAll();
        }
      });
    });

    rootEl.querySelector('[data-cmd="new"]').addEventListener('click', e => {
      const target = e.currentTarget;
      Win11.ui.showMenu(target, [
        { label: '文件夹', icon: 'folder', action: () => newItem('folder') },
        { label: '文本文档', icon: 'doc', action: () => newItem('text') }
      ], 'bottom-start');
    });
    rootEl.querySelector('[data-cmd="sort"]').addEventListener('click', e => {
      Win11.ui.showMenu(e.currentTarget, [
        { label: '名称', checked: sortBy === 'name', action: () => { sortBy = 'name'; renderContent(); } },
        { label: '修改日期', checked: sortBy === 'date', action: () => { sortBy = 'date'; renderContent(); } },
        { label: '类型', checked: sortBy === 'type', action: () => { sortBy = 'type'; renderContent(); } }
      ], 'bottom-start');
    });
    viewBtn.addEventListener('click', e => {
      Win11.ui.showMenu(e.currentTarget, [
        { label: '大图标', icon: 'grid', checked: viewMode === 'grid', action: () => { viewMode = 'grid'; renderAll(); } },
        { label: '列表', icon: 'list', checked: viewMode === 'list', action: () => { viewMode = 'list'; renderAll(); } }
      ], 'bottom-start');
    });
    rootEl.querySelector('.ex-right [data-cmd="refresh"]').addEventListener('click', () => {
      selectedId = null;
      renderAll();
      content.animate?.([{ opacity: .55 }, { opacity: 1 }], { duration: 220 });
    });
    rootEl.querySelector('[data-cmd="refresh"]:not(.ex-right *)');
    searchInput.addEventListener('input', () => { query = searchInput.value; renderContent(); });
    searchInput.addEventListener('keydown', e => {
      if (e.key === 'Escape') { searchInput.value = ''; query = ''; renderContent(); searchInput.blur(); }
    });

    content.addEventListener('keydown', e => {
      if (e.key === 'Enter' && selectedId) {
        openNode(fs.find(selectedId));
        e.preventDefault();
      }
      if (e.key === 'Delete' && selectedId) {
        const node = fs.find(selectedId);
        if (node && moveToRecycle(node)) {
          selectedId = null;
          renderAll();
        }
        e.preventDefault();
      }
      if (e.key === 'Escape') { selectedId = null; renderContent(); }
    });
    content.addEventListener('click', e => {
      if (!e.target.closest('[data-file]') && !e.target.closest('[data-open]')) {
        selectedId = null;
        content.querySelectorAll('.selected').forEach(x => x.classList.remove('selected'));
        selectedEl.textContent = '';
      }
    });

    win.addCleanup(() => {});

    renderAll();
    if (current) win.setTitle(`${current.name} - 文件资源管理器`);
  };

  /* ================================ Notepad ================================ */
  apps.notepad = function (win, opts) {
    let fileNode = opts && opts.fileId ? fs.find(opts.fileId) : null;
    let dirty = false;
    let fontSize = 13.5;
    let wrap = true;

    win.body.innerHTML = `
      <div class="np">
        <div class="np-menubar">
          <button class="np-menu-btn" data-menu="file">文件</button>
          <button class="np-menu-btn" data-menu="edit">编辑</button>
          <button class="np-menu-btn" data-menu="view">查看</button>
          <span class="grow"></span>
          <span class="np-menu-hint">Ctrl + S 保存</span>
        </div>
        <div class="np-editor-wrap">
          <textarea class="np-editor" spellcheck="false" aria-label="记事本内容"></textarea>
        </div>
        <div class="np-status">
          <span id="np-pos">第 1 行，第 1 列</span>
          <span class="grow"></span>
          <span class="np-zoom"><button data-zoom="out" title="缩小">−</button><span id="np-zoom-value">100%</span><button data-zoom="in" title="放大">+</button></span>
          <span>Windows (CRLF)</span>
          <span>UTF-8</span>
        </div>
      </div>
    `;
    const rootEl = win.body.firstElementChild;
    const editor = rootEl.querySelector('.np-editor');
    const posEl = rootEl.querySelector('#np-pos');
    const zoomEl = rootEl.querySelector('#np-zoom-value');

    if (fileNode) {
      editor.value = fileNode.content || '';
      win.setTitle(`${fileNode.name} - 记事本`);
    } else {
      editor.value = '';
      win.setTitle('无标题 - 记事本');
    }

    function updateTitle() {
      const name = fileNode ? fileNode.name : '无标题';
      win.setTitle(`${dirty ? '• ' : ''}${name} - 记事本`);
    }

    function setDirty(v) {
      if (dirty === v) return;
      dirty = v;
      updateTitle();
    }

    function updatePos() {
      const value = editor.value.slice(0, editor.selectionStart);
      const lines = value.split('\n');
      posEl.textContent = `第 ${lines.length} 行，第 ${lines[lines.length - 1].length + 1} 列`;
    }

    function save() {
      if (fileNode) {
        fs.saveText(fileNode, editor.value);
        setDirty(false);
        Win11.ui.toast('已保存', fileNode.name, { icon: Icon.app('notepad') });
        refreshDesktop();
      } else {
        Win11.ui.prompt({
          title: '另存为',
          label: '文件名',
          value: '无标题.txt',
          okText: '保存',
          onOk: name => {
            let finalName = name.trim() || '无标题.txt';
            if (!/\.[^.]+$/.test(finalName)) finalName += '.txt';
            const parent = fs.find('desktop');
            finalName = uniqueName(finalName.replace(/\.[^.]+$/, ''), parent, finalName.match(/\.[^.]+$/)[0]);
            fileNode = fs.createText(finalName, editor.value, parent);
            setDirty(false);
            refreshDesktop();
            updateTitle();
            Win11.ui.toast('已保存到桌面', finalName, { icon: Icon.app('notepad') });
          }
        });
      }
    }

    function insertText(text) {
      const start = editor.selectionStart;
      const end = editor.selectionEnd;
      editor.setRangeText(text, start, end, 'end');
      editor.focus();
      setDirty(true);
      updatePos();
    }

    function openMenu(which, anchor) {
      let items = [];
      if (which === 'file') {
        items = [
          { label: '新建窗口', icon: 'plus', action: () => Win11.WM.open('notepad') },
          { label: '打开…', icon: 'folder', action: () => Win11.WM.open('explorer', { nodeId: 'desktop' }) },
          { separator: true },
          { label: '保存', icon: 'save', shortcut: 'Ctrl+S', action: save },
          { label: '另存为…', action: () => { fileNode = null; save(); } },
          { separator: true },
          { label: '退出', icon: 'close', action: () => win.close() }
        ];
      } else if (which === 'edit') {
        items = [
          { label: '撤销', shortcut: 'Ctrl+Z', action: () => document.execCommand('undo') },
          { label: '重做', shortcut: 'Ctrl+Y', action: () => document.execCommand('redo') },
          { separator: true },
          { label: '剪切', icon: 'cut', shortcut: 'Ctrl+X', action: () => { editor.focus(); document.execCommand('cut'); setDirty(true); } },
          { label: '复制', icon: 'copy', shortcut: 'Ctrl+C', action: () => { editor.focus(); document.execCommand('copy'); } },
          { label: '粘贴', icon: 'paste', shortcut: 'Ctrl+V', action: async () => { try { const t = await navigator.clipboard.readText(); insertText(t); } catch (e) { Win11.ui.toast('无法粘贴', '浏览器没有授予剪贴板权限。'); } } },
          { separator: true },
          { label: '全选', shortcut: 'Ctrl+A', action: () => { editor.focus(); editor.select(); } },
          { label: '时间/日期', shortcut: 'F5', action: () => insertText(new Date().toLocaleString('zh-CN')) }
        ];
      } else {
        items = [
          { label: '自动换行', checked: wrap, action: () => { wrap = !wrap; rootEl.querySelector('.np').classList.toggle('nowrap', !wrap); } },
          { separator: true },
          { label: '放大', shortcut: 'Ctrl++', action: () => zoomBy(1) },
          { label: '缩小', shortcut: 'Ctrl+-', action: () => zoomBy(-1) },
          { label: '恢复默认缩放', action: () => { fontSize = 13.5; applyZoom(); } }
        ];
      }
      Win11.ui.showMenu(anchor, items, 'bottom-start');
    }

    function zoomBy(dir) {
      fontSize = Math.max(9, Math.min(28, fontSize + dir * 1.5));
      applyZoom();
    }
    function applyZoom() {
      editor.style.fontSize = fontSize + 'px';
      zoomEl.textContent = Math.round((fontSize / 13.5) * 100) + '%';
    }

    editor.addEventListener('input', () => { setDirty(true); updatePos(); });
    editor.addEventListener('click', updatePos);
    editor.addEventListener('keyup', updatePos);
    editor.addEventListener('keydown', e => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') { e.preventDefault(); save(); return; }
      if ((e.ctrlKey || e.metaKey) && (e.key === '=' || e.key === '+')) { e.preventDefault(); zoomBy(1); return; }
      if ((e.ctrlKey || e.metaKey) && e.key === '-') { e.preventDefault(); zoomBy(-1); return; }
      if (e.key === 'F5') { e.preventDefault(); insertText(new Date().toLocaleString('zh-CN')); return; }
      if (e.key === 'Tab') {
        e.preventDefault();
        insertText('    ');
      }
    });

    rootEl.querySelectorAll('[data-menu]').forEach(btn => {
      btn.addEventListener('click', () => openMenu(btn.dataset.menu, btn));
    });
    rootEl.querySelector('[data-zoom="in"]').addEventListener('click', () => zoomBy(1));
    rootEl.querySelector('[data-zoom="out"]').addEventListener('click', () => zoomBy(-1));

    win.addCleanup(() => {});
    setTimeout(() => {
      editor.focus();
      editor.setSelectionRange(editor.value.length, editor.value.length);
      updatePos();
    }, 30);
    applyZoom();
  };

  /* =============================== Calculator ============================== */
  apps.calculator = function (win) {
    win.setTitle('计算器');
    let current = '0';
    let previous = null;
    let op = null;
    let overwrite = true;
    let expr = '';
    let memory = 0;
    let error = false;
    const history = [];

    const fmt = raw => {
      if (raw === '' || raw === '-') return raw;
      const neg = raw.startsWith('-');
      const body = neg ? raw.slice(1) : raw;
      const [int, dec] = body.split('.');
      const grouped = (int || '0').replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      return (neg ? '-' : '') + grouped + (dec !== undefined ? '.' + dec : '');
    };

    win.body.innerHTML = `
      <div class="calc">
        <div class="calc-topbar">
          <button class="icon-btn" title="标准">${Icon.ui('menu')}</button>
          <span class="calc-mode">标准</span>
          <button class="icon-btn" data-calc="history" title="历史记录">${Icon.ui('time')}</button>
        </div>
        <div class="calc-display">
          <div class="calc-expr" id="calc-expr"></div>
          <div class="calc-value" id="calc-value">0</div>
        </div>
        <div class="calc-memory">
          <button data-mem="MC">MC</button><button data-mem="MR">MR</button>
          <button data-mem="M+">M+</button><button data-mem="M-">M−</button><button data-mem="MS">MS</button>
        </div>
        <div class="calc-pad">
          <button class="calc-btn fn" data-key="%">%</button>
          <button class="calc-btn fn" data-action="ce">CE</button>
          <button class="calc-btn fn" data-action="clear">C</button>
          <button class="calc-btn fn" data-action="back">⌫</button>
          <button class="calc-btn fn" data-action="inv"><sup>1</sup>⁄x</button>
          <button class="calc-btn fn" data-action="sq">x²</button>
          <button class="calc-btn fn" data-action="sqrt">√x</button>
          <button class="calc-btn fn" data-op="÷">÷</button>
          <button class="calc-btn" data-key="7">7</button>
          <button class="calc-btn" data-key="8">8</button>
          <button class="calc-btn" data-key="9">9</button>
          <button class="calc-btn fn" data-op="×">×</button>
          <button class="calc-btn" data-key="4">4</button>
          <button class="calc-btn" data-key="5">5</button>
          <button class="calc-btn" data-key="6">6</button>
          <button class="calc-btn fn" data-op="−">−</button>
          <button class="calc-btn" data-key="1">1</button>
          <button class="calc-btn" data-key="2">2</button>
          <button class="calc-btn" data-key="3">3</button>
          <button class="calc-btn fn" data-op="+">+</button>
          <button class="calc-btn fn" data-action="sign">+/−</button>
          <button class="calc-btn" data-key="0">0</button>
          <button class="calc-btn" data-key=".">.</button>
          <button class="calc-btn equals" data-action="equals">=</button>
        </div>
      </div>
    `;
    const rootEl = win.body.firstElementChild;
    const valueEl = rootEl.querySelector('#calc-value');
    const exprEl = rootEl.querySelector('#calc-expr');

    function render() {
      valueEl.textContent = error ? current : fmt(current);
      exprEl.textContent = expr;
    }

    function resetAll() {
      current = '0'; previous = null; op = null; overwrite = true; expr = ''; error = false;
    }

    function number(n) {
      if (error) resetAll();
      if (overwrite) {
        current = n === '.' ? '0.' : n;
        overwrite = false;
      } else {
        if (n === '.' && current.includes('.')) return;
        if (current.replace(/[-.]/g, '').length >= 16) return;
        if (current === '0' && n !== '.') current = n;
        else current += n;
      }
      render();
    }

    function calc(a, b, symbol) {
      const x = parseFloat(a), y = parseFloat(b);
      switch (symbol) {
        case '+': return x + y;
        case '−': return x - y;
        case '×': return x * y;
        case '÷': if (y === 0) return null; return x / y;
        default: return y;
      }
    }

    function normalize(num) {
      if (!isFinite(num)) return null;
      const rounded = Math.round(num * 1e12) / 1e12;
      return String(rounded);
    }

    function chooseOp(symbol) {
      if (error) return;
      if (op && !overwrite) {
        const res = calc(previous, current, op);
        if (res === null) { current = '无法除以零'; error = true; expr = ''; previous = null; op = null; overwrite = true; render(); return; }
        current = normalize(res);
      }
      previous = current;
      op = symbol;
      overwrite = true;
      expr = `${fmt(previous)} ${symbol}`;
      render();
    }

    function equals() {
      if (error || !op || previous === null) return;
      const shownPrevious = previous;
      const shownCurrent = current;
      const res = calc(previous, current, op);
      if (res === null) {
        current = '无法除以零'; error = true; expr = ''; previous = null; op = null; overwrite = true; render(); return;
      }
      current = normalize(res);
      expr = `${fmt(shownPrevious)} ${op} ${fmt(shownCurrent)} =`;
      const item = { expr: `${fmt(shownPrevious)} ${op} ${fmt(shownCurrent)} =`, result: fmt(current) };
      history.unshift(item);
      if (history.length > 30) history.pop();
      previous = null;
      op = null;
      overwrite = true;
      render();
    }

    function action(act) {
      if (act === 'clear') { resetAll(); render(); return; }
      if (error) resetAll();
      switch (act) {
        case 'ce': current = '0'; overwrite = true; break;
        case 'back':
          if (overwrite) break;
          current = current.slice(0, -1);
          if (!current || current === '-') current = '0';
          break;
        case 'sign':
          current = current.startsWith('-') ? current.slice(1) : '-' + current;
          if (current === '-0') current = '0';
          break;
        case 'inv': {
          const v = parseFloat(current);
          if (v === 0) { current = '无法除以零'; error = true; break; }
          current = normalize(1 / v);
          expr = `1/(${fmt(previous || '')})`.replace('()', '');
          overwrite = true;
          break;
        }
        case 'sq': current = normalize(Math.pow(parseFloat(current), 2)); expr = `sqr(${fmt(current)})`; overwrite = true; break;
        case 'sqrt': {
          const v = parseFloat(current);
          if (v < 0) { current = '无效输入'; error = true; break; }
          current = normalize(Math.sqrt(v)); expr = `√(${fmt(current)})`; overwrite = true;
          break;
        }
        case 'equals': equals(); break;
      }
      render();
    }

    function memoryAction(act) {
      const v = parseFloat(current) || 0;
      if (act === 'MS') memory = v;
      if (act === 'MC') memory = 0;
      if (act === 'M+') memory += v;
      if (act === 'M-') memory -= v;
      if (act === 'MR') { current = normalize(memory); overwrite = true; render(); return; }
      Win11.ui.toast('计算器内存', `M = ${fmt(normalize(memory))}`, { icon: Icon.app('calculator') });
    }

    rootEl.querySelectorAll('[data-key]').forEach(btn => btn.addEventListener('click', () => {
      const key = btn.dataset.key;
      if (key === '.') number('.');
      else number(key);
    }));
    rootEl.querySelectorAll('[data-op]').forEach(btn => btn.addEventListener('click', () => chooseOp(btn.dataset.op)));
    rootEl.querySelectorAll('[data-action]').forEach(btn => btn.addEventListener('click', () => action(btn.dataset.action)));
    rootEl.querySelectorAll('[data-mem]').forEach(btn => btn.addEventListener('click', () => memoryAction(btn.dataset.mem)));

    const histBtn = rootEl.querySelector('[data-calc="history"]');
    histBtn.addEventListener('click', () => {
      if (rootEl.querySelector('.calc-history')) { rootEl.querySelector('.calc-history').remove(); return; }
      const panel = document.createElement('div');
      panel.className = 'calc-history';
      panel.innerHTML = `
        <div class="calc-history-head"><span>历史记录</span><button class="icon-btn" data-close>${Icon.ui('close')}</button></div>
        <div class="calc-history-list">
          ${history.length ? history.map(h => `<div class="calc-history-row"><small>${U.escapeHtml(h.expr)}</small><strong>${U.escapeHtml(h.result)}</strong></div>`).join('') : '<div class="calc-history-empty">这里还没有计算记录。</div>'}
        </div>`;
      rootEl.appendChild(panel);
      panel.querySelector('[data-close]').addEventListener('click', () => panel.remove());
    });

    const onKey = e => {
      if (!win.isFocused()) return;
      const t = e.target;
      if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) return;
      const k = e.key;
      if (/^[0-9]$/.test(k)) { number(k); e.preventDefault(); }
      else if (k === '.' || k === ',') { number('.'); e.preventDefault(); }
      else if (k === '+') { chooseOp('+'); e.preventDefault(); }
      else if (k === '-') { chooseOp('−'); e.preventDefault(); }
      else if (k === '*') { chooseOp('×'); e.preventDefault(); }
      else if (k === '/') { chooseOp('÷'); e.preventDefault(); e.preventDefault(); }
      else if (k === 'Enter' || k === '=') { equals(); e.preventDefault(); }
      else if (k === 'Backspace') { action('back'); e.preventDefault(); }
      else if (k === 'Escape') { resetAll(); render(); e.preventDefault(); }
      else if (k === '%') { current = normalize(parseFloat(current) / 100); overwrite = true; render(); e.preventDefault(); }
    };
    document.addEventListener('keydown', onKey);
    win.addCleanup(() => document.removeEventListener('keydown', onKey));

    render();
  };

  /* ================================ Settings =============================== */
  apps.settings = function (win, opts) {
    const Store = Win11.Store;
    let section = (opts && opts.section) || 'system';

    const navItems = [
      { id: 'system', label: '系统', icon: 'display' },
      { id: 'bluetooth', label: '蓝牙和设备', icon: 'bluetooth' },
      { id: 'network', label: '网络和 Internet', icon: 'globe' },
      { id: 'personalization', label: '个性化', icon: 'palette' },
      { id: 'apps', label: '应用', icon: 'apps' },
      { id: 'accounts', label: '账户', icon: 'user' },
      { id: 'time', label: '时间和语言', icon: 'time' },
      { id: 'privacy', label: '隐私和安全性', icon: 'privacy' },
      { id: 'update', label: 'Windows 更新', icon: 'update' },
      { id: 'about', label: '关于', icon: 'info' }
    ];

    win.body.innerHTML = `
      <div class="settings">
        <aside class="settings-sidebar">
          <div class="settings-user">
            <span class="avatar" id="set-avatar"></span>
            <span><span class="settings-user-name" id="set-user-name"></span><span class="settings-user-sub">本地账户</span></span>
          </div>
          <nav id="set-nav"></nav>
        </aside>
        <div class="settings-content" id="set-content"></div>
      </div>
    `;
    const rootEl = win.body.firstElementChild;
    const nav = rootEl.querySelector('#set-nav');
    const content = rootEl.querySelector('#set-content');
    const avatar = rootEl.querySelector('#set-avatar');
    const userName = rootEl.querySelector('#set-user-name');

    const wall = () => Win11.wallpaperOf(Store.state.wallpaper);
    const toggle = (key, checked) => `<button class="pill-toggle" role="switch" aria-checked="${checked}" data-toggle="${key}" aria-label="${key}"></button>`;

    function refreshUser() {
      const name = Win11.userName();
      avatar.textContent = Win11.initialOf(name);
      avatar.style.background = `linear-gradient(145deg, ${U.hexToRgba(Store.state.accent, .65)}, ${Store.state.accent})`;
      userName.textContent = name;
    }

    function renderNav() {
      nav.innerHTML = navItems.map(item => `
        <button class="set-nav-item ${section === item.id ? 'active' : ''}" data-section="${item.id}">
          ${Icon.ui(item.icon)}<span>${item.label}</span>
        </button>`).join('');
      nav.querySelectorAll('[data-section]').forEach(btn => {
        btn.addEventListener('click', () => setSection(btn.dataset.section));
      });
    }

    function pageHeader(title, sub) {
      return `<h1 class="set-page-title">${title}</h1><div class="set-page-sub">${sub}</div>`;
    }

    function hero() {
      return `<div class="set-hero" style="background-image:url('${wall().file}')">
        <div class="set-hero-text"><strong>${winName()}，欢迎回来</strong><span>当前壁纸：${wall().name} · 让桌面保持你喜欢的样子</span></div>
      </div>`;
    }

    function winName() { return Win11.userName(); }

    const pages = {
      system() {
        return pageHeader('系统', 'DESKTOP-WEB11 · Windows 11 专业版') + hero() + `
          <div class="set-info-strip">${Icon.ui('info')}<span>这是离线演示桌面，设置会保存在浏览器本地存储中。</span></div>
          <div class="set-card">
            <div class="set-card-head"><h3>显示</h3><p>亮度、夜间模式与桌面预览</p></div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">亮度</div><div class="set-row-sub">调整整个屏幕的亮度</div></div>
              <div class="set-slider"><input type="range" min="25" max="100" step="1" value="${Store.state.brightness}" id="set-brightness" aria-label="亮度"></div>
              <span class="tiny-meta" id="set-brightness-val" style="width:34px;text-align:right">${Store.state.brightness}%</span>
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">夜间模式</div><div class="set-row-sub">让屏幕颜色更温暖，减少夜间蓝光</div></div>
              ${toggle('nightLight', Store.state.nightLight)}
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">分辨率</div><div class="set-row-sub">当前显示器的原始分辨率</div></div>
              <span class="tiny-meta">1920 × 1080（推荐）</span>
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">缩放</div><div class="set-row-sub">更改文本、应用和其他项目的大小</div></div>
              <select aria-label="缩放"><option>100%（推荐）</option><option>125%</option><option>150%</option></select>
            </div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>声音</h3><p>系统输出音量</p></div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">主音量</div><div class="set-row-sub">扬声器（Realtek 音频）</div></div>
              <div class="set-slider"><input type="range" min="0" max="100" step="1" value="${Store.state.volume}" id="set-volume" aria-label="音量"></div>
              <span class="tiny-meta" id="set-volume-val" style="width:34px;text-align:right">${Store.state.volume}%</span>
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">节电模式</div><div class="set-row-sub">降低后台活动与屏幕亮度</div></div>
              ${toggle('saver', Store.state.saver)}
            </div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>关于此电脑</h3></div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">设备名称</div><div class="set-row-sub">DESKTOP-WEB11</div></div>
              <button class="secondary-btn" data-goto="about">查看详情</button>
            </div>
          </div>`;
      },

      personalization() {
        return pageHeader('个性化', '让桌面、颜色和任务栏更符合你的习惯。') + hero() + `
          <div class="set-card">
            <div class="set-card-head"><h3>背景</h3><p>选择一张你喜欢的壁纸。所有壁纸均为本示例原创绘制。</p></div>
            <div class="wallpaper-grid">
              ${Win11.wallpapers.map(w => `
                <button class="wp-thumb ${Store.state.wallpaper === w.id ? 'active' : ''}" data-wallpaper="${w.id}" style="background-image:url('${w.file}')" aria-label="${w.name}">
                  <span>${w.name}</span><span class="wp-check">${Icon.ui('check')}</span>
                </button>`).join('')}
            </div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>颜色</h3><p>选择模式与强调色，强调色会用于按钮、开关和任务栏指示器。</p></div>
            <div class="theme-grid">
              ${['light', 'dark'].map(t => `
                <button class="theme-card ${Store.state.theme === t ? 'active' : ''}" data-theme-choice="${t}">
                  <div class="theme-preview ${t}"></div>
                  <div class="theme-card-label">${Icon.ui(t === 'light' ? 'sun' : 'moon')}<span>${t === 'light' ? '浅色' : '深色'}</span></div>
                </button>`).join('')}
            </div>
            <div class="accent-row">
              ${Win11.accents.map(a => `<button class="accent-dot ${Store.state.accent === a.value ? 'active' : ''}" data-accent="${a.value}" style="background:${a.value}" title="${a.name}" aria-label="${a.name}"></button>`).join('')}
            </div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>任务栏</h3><p>选择任务栏图标与系统托盘的对齐方式。</p></div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">任务栏对齐方式</div><div class="set-row-sub">居中或靠左显示开始与应用图标</div></div>
              <div class="segmented">
                <button data-align="center" class="${Store.state.align === 'center' ? 'active' : ''}">居中</button>
                <button data-align="left" class="${Store.state.align === 'left' ? 'active' : ''}">左对齐</button>
              </div>
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">显示桌面按钮</div><div class="set-row-sub">点击任务栏最右侧可最小化所有窗口</div></div>
              <button class="pill-toggle" role="switch" aria-checked="true" aria-label="显示桌面按钮"></button>
            </div>
          </div>`;
      },

      accounts() {
        return pageHeader('账户', '管理你的本地账户、头像和登录选项。') + `
          <div class="set-card">
            <div class="about-hero">
              <span class="avatar" style="width:58px;height:58px;font-size:23px;background:linear-gradient(145deg, ${U.hexToRgba(Store.state.accent, .65)}, ${Store.state.accent})">${Win11.initialOf()}</span>
              <div><div class="about-name">${U.escapeHtml(Win11.userName())}</div><div class="about-sub">本地账户 · 管理员</div></div>
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">你的名字</div><div class="set-row-sub">显示在开始菜单、锁屏和设置中</div></div>
              <input type="text" id="set-name-input" value="${U.escapeHtml(Win11.userName())}" maxlength="24" aria-label="你的名字">
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">改用 Microsoft 账户登录</div><div class="set-row-sub">演示模式不需要联网，也不会发送任何数据</div></div>
              <button class="secondary-btn" data-toast="这是一个离线演示，本地账户已经足够啦。">了解更多</button>
            </div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>登录选项</h3></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">Windows Hello</div><div class="set-row-sub">面部识别或指纹在此设备上不可用</div></div><span class="tiny-meta">不可用</span></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">PIN</div><div class="set-row-sub">登录锁屏时可以使用任意 PIN 或直接按 Enter</div></div><button class="secondary-btn" data-toast="演示模式下，直接按 Enter 就能登录。">管理</button></div>
          </div>`;
      },

      apps() {
        const installed = ['explorer', 'notepad', 'calculator', 'terminal', 'settings', 'photos', 'weather', 'recycle'];
        return pageHeader('应用', '已安装的应用与默认应用。') + `
          <div class="set-card">
            <div class="set-card-head"><h3>已安装的应用</h3><p>这些应用都随桌面一起离线运行。</p></div>
            ${installed.map(id => {
              const a = Win11.appsMeta[id];
              return `<div class="app-list-row">${Icon.app(a.icon)}<span>${a.name}</span><span class="faint">${id === 'recycle' ? '系统' : '内置'} · 本地应用</span></div>`;
            }).join('')}
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>默认应用</h3></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">文本文件</div><div class="set-row-sub">.txt 和 .md 默认使用记事本打开</div></div><span class="tiny-meta">记事本</span></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">图片</div><div class="set-row-sub">SVG 图片默认使用照片打开</div></div><span class="tiny-meta">照片</span></div>
          </div>`;
      },

      network() {
        return pageHeader('网络和 Internet', 'Wi-Fi、飞行模式与当前连接状态。') + `
          <div class="set-card">
            <div class="set-card-head"><h3>Wi-Fi</h3><p>连接到附近的无线网络。</p></div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">Wi-Fi</div><div class="set-row-sub">${Store.state.wifi ? '已连接到 Contoso Wi-Fi' : '已关闭'}</div></div>
              ${toggle('wifi', Store.state.wifi)}
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">Contoso Wi-Fi</div><div class="set-row-sub">${Store.state.wifi ? '已连接，安全 · 5 GHz · 信号良好' : '未连接'}</div></div>
              <span class="tiny-meta">${Store.state.wifi ? '已连接' : '—'}</span>
            </div>
            ${Store.state.wifi ? `<div class="set-row"><div class="set-row-main"><div class="set-row-title">IP 地址</div><div class="set-row-sub">192.168.1.42 · DNS 192.168.1.1</div></div></div>` : ''}
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>飞行模式</h3></div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">飞行模式</div><div class="set-row-sub">关闭所有无线通信</div></div>
              ${toggle('airplane', Store.state.airplane)}
            </div>
          </div>`;
      },

      bluetooth() {
        return pageHeader('蓝牙和设备', '连接鼠标、键盘、耳机和其他设备。') + `
          <div class="set-card">
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">蓝牙</div><div class="set-row-sub">${Store.state.bluetooth ? '现在可被发现' : '已关闭'}</div></div>
              ${toggle('bluetooth', Store.state.bluetooth)}
            </div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>已连接的设备</h3><p>暂无蓝牙设备。这只是一个网页桌面。</p></div>
            <div class="recycle-empty" style="height:190px">
              ${Icon.ui('bluetooth')}
              <div>没有找到设备</div>
              <button class="secondary-btn" data-toast="演示模式不支持搜索附近的蓝牙设备。">添加设备</button>
            </div>
          </div>`;
      },

      time() {
        const now = new Date();
        return pageHeader('时间和语言', '日期、时间、区域和显示语言。') + `
          <div class="set-card">
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">当前时间</div><div class="set-row-sub">${now.toLocaleString('zh-CN', { hour12: false })}</div></div><span class="tiny-meta">自动设置</span></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">时区</div><div class="set-row-sub">(UTC+08:00) 北京，重庆，香港特别行政区，乌鲁木齐</div></div></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">自动设置时间</div><div class="set-row-sub">让 Windows 自动校准时间</div></div>${toggle('autoTime', Store.state.autoTime !== false)}</div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>语言</h3></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">Windows 显示语言</div><div class="set-row-sub">简体中文</div></div><span class="tiny-meta">中文（简体）</span></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">首选语言</div><div class="set-row-sub">简体中文（中国）</div></div></div>
          </div>`;
      },

      privacy() {
        const state = Store.state;
        return pageHeader('隐私和安全性', '控制应用可以访问的内容。本页面不会请求任何真实权限。') + `
          <div class="set-info-strip">${Icon.ui('shield')}<span>这个桌面不会访问你的位置、相机、麦克风或任何本地记录。</span></div>
          <div class="set-card">
            <div class="set-card-head"><h3>权限</h3></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">位置</div><div class="set-row-sub">允许应用访问精确位置</div></div>${toggle('privacyLocation', state.privacyLocation !== false)}</div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">相机</div><div class="set-row-sub">允许应用使用相机</div></div>${toggle('privacyCamera', state.privacyCamera === true)}</div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">麦克风</div><div class="set-row-sub">允许应用使用麦克风</div></div>${toggle('privacyMic', state.privacyMic === true)}</div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>Windows 安全中心</h3></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">设备安全性</div><div class="set-row-sub">你的设备已受到保护，未发现威胁</div></div><span class="tiny-meta" style="color:#3e9e6a">一切正常</span></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">防火墙和网络保护</div><div class="set-row-sub">域网络、专用网络和公用网络均已启用</div></div></div>
          </div>`;
      },

      update() {
        return pageHeader('Windows 更新', '保持你的设备安全、稳定并顺畅运行。') + `
          <div class="set-card">
            <div class="about-hero" style="padding-bottom:6px">
              <span class="about-logo" style="width:54px;height:54px;border-radius:50%;background:var(--accent-soft)">${Icon.ui('check')}</span>
              <div><div class="about-name" style="font-size:16px">你已是最新版本</div><div class="about-sub">上次检查：今天 09:12 · Windows 11 24H2</div></div>
            </div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">检查更新</div><div class="set-row-sub">我们会在有新的安全更新时通知你</div></div><button class="secondary-btn" data-action="check-update">检查更新</button></div>
          </div>
          <div class="set-card">
            <div class="set-card-head"><h3>更新历史记录</h3></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">2026-11 适用于 Windows 11 的累积更新</div><div class="set-row-sub">已成功安装 · 26100.2314 · 2026/11/12</div></div><span class="tiny-meta">成功</span></div>
            <div class="set-row"><div class="set-row-main"><div class="set-row-title">Web Experience Pack</div><div class="set-row-sub">已成功安装 · 1000.26100.38.0 · 2026/11/01</div></div><span class="tiny-meta">成功</span></div>
          </div>`;
      },

      about() {
        return pageHeader('关于', '设备规格与 Windows 版本信息。') + `
          <div class="set-card">
            <div class="about-hero">
              <span class="about-logo">${Icon.app('win')}</span>
              <div><div class="about-name">DESKTOP-WEB11</div><div class="about-sub">Windows 11 专业版 · 24H2</div></div>
            </div>
            <div class="spec-list">
              <div class="spec-row"><span class="spec-label">设备名称</span><span class="spec-value">DESKTOP-WEB11</span></div>
              <div class="spec-row"><span class="spec-label">处理器</span><span class="spec-value">Web Core i9-13900K @ 5.20 GHz（模拟）</span></div>
              <div class="spec-row"><span class="spec-label">机带 RAM</span><span class="spec-value">32.0 GB（31.8 GB 可用）</span></div>
              <div class="spec-row"><span class="spec-label">系统类型</span><span class="spec-value">64 位操作系统，基于 x64 的处理器</span></div>
              <div class="spec-row"><span class="spec-label">设备 ID</span><span class="spec-value">7F3A9C21-4B6E-4C2A-9E10-11E2D8A4C001</span></div>
              <div class="spec-row"><span class="spec-label">产品 ID</span><span class="spec-value">00330-80000-00000-AA874</span></div>
              <div class="spec-row"><span class="spec-label">安装日期</span><span class="spec-value">2026/10/28</span></div>
              <div class="spec-row"><span class="spec-label">OS 内部版本</span><span class="spec-value">26100.2314</span></div>
              <div class="spec-row"><span class="spec-label">体验</span><span class="spec-value">Windows Feature Experience Pack 1000.26100.38.0</span></div>
            </div>
            <div class="set-row">
              <div class="set-row-main"><div class="set-row-title">恢复默认设置</div><div class="set-row-sub">壁纸、主题、强调色、用户名和亮度将恢复默认值</div></div>
              <button class="secondary-btn" data-action="reset">恢复默认</button>
            </div>
          </div>
          <div class="set-info-strip">${Icon.ui('info')}<span>这是一个独立的个人演示项目，与 Microsoft 无关。所有图标和壁纸均由本示例绘制。</span></div>`;
      }
    };

    function setSection(id) {
      section = id;
      renderNav();
      content.innerHTML = pages[id] ? pages[id]() : '';
      content.scrollTop = 0;
      bindPage();
    }

    function bindPage() {
      const brightness = content.querySelector('#set-brightness');
      if (brightness) {
        brightness.addEventListener('input', () => {
          Store.set('brightness', Number(brightness.value));
          const val = content.querySelector('#set-brightness-val');
          if (val) val.textContent = brightness.value + '%';
          brightness.style.setProperty('--range-progress', brightness.value + '%');
        });
        brightness.style.setProperty('--range-progress', Store.state.brightness + '%');
      }
      const volume = content.querySelector('#set-volume');
      if (volume) {
        volume.addEventListener('input', () => {
          Store.set('volume', Number(volume.value));
          const val = content.querySelector('#set-volume-val');
          if (val) val.textContent = volume.value + '%';
          volume.style.setProperty('--range-progress', volume.value + '%');
        });
        volume.style.setProperty('--range-progress', Store.state.volume + '%');
      }
      const nameInput = content.querySelector('#set-name-input');
      if (nameInput) {
        nameInput.addEventListener('keydown', e => { if (e.key === 'Enter') nameInput.blur(); });
        nameInput.addEventListener('change', () => {
          const name = nameInput.value.trim() || 'Aki';
          Store.set('userName', name);
          refreshUser();
          Win11.ui.toast('已更新账户名称', `现在称呼你为「${name}」。`, { icon: Icon.app('settings') });
        });
      }
    }

    content.addEventListener('click', e => {
      const wp = e.target.closest('[data-wallpaper]');
      if (wp) {
        Store.set('wallpaper', wp.dataset.wallpaper);
        Win11.ui.toast('个性化', `桌面背景已更换为「${wall().name}」。`, { icon: Icon.app('photos') });
        setSection('personalization');
        return;
      }
      const th = e.target.closest('[data-theme-choice]');
      if (th) {
        Store.set('theme', th.dataset.themeChoice);
        Win11.ui.toast('主题已更新', Store.state.theme === 'dark' ? '已切换到深色模式。' : '已切换到浅色模式。', { icon: Icon.app('settings') });
        setSection('personalization');
        return;
      }
      const acc = e.target.closest('[data-accent]');
      if (acc) {
        Store.set('accent', acc.dataset.accent);
        refreshUser();
        setSection('personalization');
        return;
      }
      const align = e.target.closest('[data-align]');
      if (align) {
        Store.set('align', align.dataset.align);
        setSection('personalization');
        return;
      }
      const tog = e.target.closest('[data-toggle]');
      if (tog) {
        const key = tog.dataset.toggle;
        if (key === 'airplane') {
          const next = !Store.state.airplane;
          Store.patch({ airplane: next, ...(next ? { wifi: false, bluetooth: false } : {}) });
        } else if (key === 'wifi' || key === 'bluetooth') {
          const next = !Store.state[key];
          Store.patch({ [key]: next, ...(next ? { airplane: false } : {}) });
          if (next) Win11.ui.toast(key === 'wifi' ? '已连接到 Contoso Wi-Fi' : '蓝牙已开启', key === 'wifi' ? '网络连接正常，可以离线浏览这个桌面。' : '现在可以发现附近的设备。', { icon: Icon.ui(key === 'wifi' ? 'wifi' : 'bluetooth') });
        } else if (key === 'autoTime') {
          Store.set('autoTime', Store.state.autoTime === false);
        } else {
          Store.set(key, !Store.state[key]);
        }
        setSection(section);
        return;
      }
      const gotoBtn = e.target.closest('[data-goto]');
      if (gotoBtn) { setSection(gotoBtn.dataset.goto); return; }
      const actionBtn = e.target.closest('[data-action]');
      if (actionBtn) {
        const act = actionBtn.dataset.action;
        if (act === 'reset') {
          Win11.ui.confirm({
            title: '恢复默认设置？',
            message: '壁纸、主题、强调色、用户名、亮度和开关状态都会恢复默认值。浏览器中保存的记事本文本不会被删除。',
            okText: '恢复默认',
            danger: true,
            onOk: () => {
              Store.reset();
              Win11.ui.toast('设置已恢复', '桌面已经回到初始状态。', { icon: Icon.app('settings') });
              refreshUser();
              setSection('system');
            }
          });
        }
        if (act === 'check-update') {
          actionBtn.disabled = true;
          actionBtn.textContent = '正在检查…';
          setTimeout(() => {
            actionBtn.disabled = false;
            actionBtn.textContent = '检查更新';
            Win11.ui.toast('Windows 更新', '你已拥有最新版本，无需更新。', { icon: Icon.ui('update') });
          }, 1100);
        }
        return;
      }
      const toastBtn = e.target.closest('[data-toast]');
      if (toastBtn) Win11.ui.toast('提示', toastBtn.dataset.toast, { icon: Icon.app('settings') });
    });

    document.addEventListener('win11:settings', refreshUser);
    win.addCleanup(() => document.removeEventListener('win11:settings', refreshUser));
    win.update = opts2 => { if (opts2 && opts2.section) setSection(opts2.section); };

    win.setTitle('设置');
    refreshUser();
    setSection(section);
  };

  /* ================================ Terminal =============================== */
  apps.terminal = function (win) {
    win.setTitle('Windows PowerShell');
    let cwd = 'C:\\Users\\Aki';
    const history = [];
    let histIndex = -1;

    win.body.innerHTML = `
      <div class="term">
        <div class="term-tabs">
          <div class="term-tab">${Icon.app('terminal')}<span>Windows PowerShell</span><span class="term-tab-close">${Icon.ui('close')}</span></div>
          <button class="term-newtab" title="新建标签页">${Icon.ui('plus')}</button>
        </div>
        <div class="term-body" id="term-body"></div>
      </div>
    `;
    const rootEl = win.body.firstElementChild;
    const body = rootEl.querySelector('#term-body');

    function promptText() {
      return `PS ${cwd}> `;
    }

    let input = null;
    function scrollBottom() { body.scrollTop = body.scrollHeight; }

    function addLine(text, cls) {
      const div = document.createElement('div');
      div.className = 'term-line' + (cls ? ' ' + cls : '');
      div.textContent = text;
      body.appendChild(div);
      return div;
    }

    function addHTML(html, cls) {
      const div = document.createElement('div');
      div.className = 'term-line' + (cls ? ' ' + cls : '');
      div.innerHTML = html;
      body.appendChild(div);
      return div;
    }

    function removeCurrentPrompt() {
      const row = body.querySelector('.term-prompt-row');
      if (row) row.remove();
    }

    function newPrompt() {
      removeCurrentPrompt();
      const row = document.createElement('div');
      row.className = 'term-prompt-row';
      row.innerHTML = `<span class="term-prompt">PS <span class="term-path">${U.escapeHtml(cwd)}</span>&gt;&nbsp;</span>`;
      input = document.createElement('input');
      input.className = 'term-input';
      input.setAttribute('aria-label', '终端命令');
      input.spellcheck = false;
      row.appendChild(input);
      body.appendChild(row);
      input.addEventListener('keydown', onInputKey);
      input.focus();
      scrollBottom();
    }

    function onInputKey(e) {
      if (e.key === 'Enter') {
        const cmd = input.value;
        input.value = '';
        runCommand(cmd);
        e.preventDefault();
      } else if (e.key === 'ArrowUp') {
        if (!history.length) return;
        histIndex = Math.min(history.length - 1, histIndex + 1);
        input.value = history[history.length - 1 - histIndex];
        e.preventDefault();
      } else if (e.key === 'ArrowDown') {
        if (histIndex <= 0) { histIndex = -1; input.value = ''; return; }
        histIndex -= 1;
        input.value = history[history.length - 1 - histIndex];
        e.preventDefault();
      } else if (e.key === 'l' && e.ctrlKey) {
        body.innerHTML = '';
        newPrompt();
        e.preventDefault();
      }
    }

    function echo(cmd) {
      const path = U.escapeHtml(cwd);
      addHTML(`<span class="term-prompt">PS <span class="term-path">${path}</span>&gt;</span> ${U.escapeHtml(cmd)}`, 'cmd');
    }

    function runCommand(raw) {
      const cmd = raw.trim();
      echo(raw);
      if (cmd) history.push(cmd);
      histIndex = -1;
      if (!cmd) { newPrompt(); return; }
      const parts = cmd.split(/\s+/);
      const base = parts[0].toLowerCase();
      const arg = cmd.slice(parts[0].length).trim();

      if (base === 'help') {
        addLine('可用命令：', 'accent');
        addLine('  help                 显示此帮助');
        addLine('  dir / ls             列出当前目录');
        addLine('  cd <路径>            切换目录');
        addLine('  echo <文本>          输出文本');
        addLine('  whoami / hostname    当前用户与计算机名');
        addLine('  ver                  显示 Windows 版本');
        addLine('  date / time          显示日期或时间');
        addLine('  neofetch             系统信息小卡片');
        addLine('  start <应用>         打开应用（explorer、notepad、calc、settings、photos、weather）');
        addLine('  theme light|dark     切换系统主题');
        addLine('  ipconfig             查看网络配置');
        addLine('  cls / clear          清屏');
        addLine('  exit                 关闭终端');
      } else if (base === 'dir' || base === 'ls') {
        addLine(`    目录: ${cwd}`, 'dim');
        addLine('');
        addLine('Mode                 LastWriteTime         Length Name', 'dim');
        addLine('----                 -------------         ------ ----', 'dim');
        addLine('d-----        2026/11/13     09:20                Desktop');
        addLine('d-----        2026/11/12     17:30                Documents');
        addLine('d-----        2026/11/10     18:02                Pictures');
        addLine('-a----        2026/11/13     09:20           1420 welcome.txt');
        addLine('');
      } else if (base === 'cd') {
        if (!arg) addLine(cwd);
        else if (arg === '\\' || arg === '/') cwd = 'C:\\';
        else if (arg === '..') {
          if (cwd === 'C:\\') return newPrompt();
          const bits = cwd.split('\\');
          bits.pop();
          cwd = bits.length > 1 ? bits.join('\\') : 'C:\\';
          if (!cwd.endsWith('\\') && cwd.length === 2) cwd += '\\';
        } else if (arg === '~') cwd = 'C:\\Users\\Aki';
        else {
          const clean = arg.replace(/\//g, '\\');
          if (clean.match(/^[a-zA-Z]:\\/)) cwd = clean.replace(/\\+$/, '') || 'C:\\';
          else {
            if (cwd.endsWith('\\')) cwd = cwd.slice(0, -1);
            cwd = `${cwd}\\${clean}`;
          }
        }
      } else if (base === 'echo') {
        addLine(arg);
      } else if (base === 'whoami') {
        addLine('desktop-web11\\aki');
      } else if (base === 'hostname') {
        addLine('DESKTOP-WEB11');
      } else if (base === 'ver') {
        addLine('');
        addLine('Microsoft Windows [版本 10.0.26100.2314]');
        addLine('© Microsoft Corporation。保留所有权利。（演示文本）');
        addLine('');
      } else if (base === 'date') {
        addLine(new Date().toLocaleDateString('zh-CN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }));
      } else if (base === 'time') {
        addLine(new Date().toLocaleTimeString('zh-CN', { hour12: false }));
      } else if (base === 'cls' || base === 'clear') {
        body.innerHTML = '';
      } else if (base === 'neofetch') {
        addHTML(`<pre class="term-ascii">  ██╗    ██╗██╗███╗   ██╗
  ██║    ██║██║████╗  ██║
  ██║ █╗ ██║██║██╔██╗ ██║
  ██║███╗██║██║██║╚██╗██║
  ╚███╔███╔╝██║██║ ╚████║
   ╚══╝╚══╝ ╚═╝╚═╝  ╚═══╝</pre>`);
        addLine('Aki@DESKTOP-WEB11', 'accent');
        addLine('------------------', 'dim');
        addLine('OS      Windows 11 Pro 24H2 (web)');
        addLine('Kernel  11.0.26100.2314');
        addLine('Shell   PowerShell 7.4 (demo)');
        addLine('Theme   ' + (Win11.Store.state.theme === 'dark' ? 'Dark / ' : 'Light / ') + Win11.wallpaperOf(Win11.Store.state.wallpaper).name);
        addLine('CPU     Web Core i9-13900K (16) @ 5.2 GHz');
        addLine('Memory  8.2 GiB / 32.0 GiB');
        addLine('Display 1920x1080 · 60 Hz');
        addLine('Uptime  今天已运行 3 小时 21 分钟');
      } else if (base === 'ipconfig') {
        addLine('');
        addLine('Windows IP 配置', 'accent');
        addLine('');
        addLine('无线局域网适配器 WLAN:');
        addLine('   连接特定的 DNS 后缀 . . . . . . . : localdomain');
        addLine('   IPv4 地址 . . . . . . . . . . . . : 192.168.1.42');
        addLine('   子网掩码  . . . . . . . . . . . . : 255.255.255.0');
        addLine('   默认网关. . . . . . . . . . . . . : 192.168.1.1');
        addLine('');
      } else if (base === 'start') {
        const map = {
          explorer: 'explorer', files: 'explorer', 文件: 'explorer',
          notepad: 'notepad', 记事本: 'notepad',
          calc: 'calculator', calculator: 'calculator', 计算器: 'calculator',
          terminal: 'terminal', 终端: 'terminal',
          settings: 'settings', 设置: 'settings',
          photos: 'photos', 照片: 'photos',
          weather: 'weather', 天气: 'weather'
        };
        const app = map[arg.toLowerCase()] || map[arg];
        if (app) {
          Win11.WM.open(app);
          addLine(`已启动 ${Win11.appsMeta[app].name}。`, 'success');
        } else {
          addLine(`找不到应用“${arg || ''}”。试试 start notepad。`, 'error');
        }
      } else if (base === 'theme') {
        if (arg === 'light' || arg === 'dark') {
          Win11.Store.set('theme', arg);
          addLine(`主题已切换为 ${arg === 'dark' ? '深色' : '浅色'}。`, 'success');
        } else addLine('用法：theme light | dark', 'dim');
      } else if (base === 'exit') {
        win.close();
        return;
      } else {
        addLine(`无法将“${parts[0]}”项识别为 cmdlet、函数、脚本文件或可运行程序的名称。请检查名称的拼写，如果包括路径，请确保路径正确，然后再试一次。`, 'error');
        addLine('输入 help 查看演示终端支持的可用命令。', 'dim');
      }
      newPrompt();
    }

    body.addEventListener('click', e => {
      if (window.getSelection().toString()) return;
      if (e.target.tagName !== 'INPUT' && input) input.focus();
    });
    rootEl.querySelector('.term-tab-close').addEventListener('click', e => { e.stopPropagation(); win.close(); });
    rootEl.querySelector('.term-newtab').addEventListener('click', () => Win11.WM.open('terminal'));

    addLine('Windows PowerShell（网页演示版）', 'accent');
    addLine('Copyright (C) Microsoft Corporation。保留所有权利。（演示文本）', 'dim');
    addLine('');
    addLine('输入 help 查看可用命令，输入 neofetch 查看系统信息。', 'dim');
    addLine('');
    newPrompt();
    setTimeout(() => input && input.focus(), 30);
  };

  /* ================================= Photos ================================ */
  apps.photos = function (win, opts) {
    const pictures = fs.find('pictures');
    let images = pictures ? pictures.children.filter(n => n.kind === 'image') : [];
    if (opts && opts.fileId) {
      const extra = fs.find(opts.fileId);
      if (extra && extra.kind === 'image' && !images.some(n => n.image === extra.image)) images = [extra, ...images];
    }
    let viewerIndex = -1;

    win.body.innerHTML = `
      <div class="photos-app">
        <div class="photos-head">
          <span class="photos-title">照片</span>
          <span class="grow"></span>
          <button class="photos-tool">${Icon.ui('plus')}<span>新建</span></button>
          <button class="photos-tool">${Icon.ui('pin')}<span>收藏</span></button>
          <button class="photos-tool">${Icon.ui('more')}</button>
        </div>
        <div class="photos-body" id="photos-body">
          <div class="photos-date">2026 年 11 月</div>
          <div class="photos-gallery" id="photos-gallery"></div>
        </div>
        <div class="photos-viewer" id="photos-viewer" hidden>
          <div class="photos-viewer-top">
            <button class="icon-btn" data-photo="close" title="关闭">${Icon.ui('close')}</button>
            <span class="photos-viewer-title" id="photo-title"></span>
            <span class="tiny-meta" style="color:rgba(255,255,255,.45)" id="photo-count"></span>
          </div>
          <div class="photos-canvas">
            <button class="photos-arrow prev" data-photo="prev" aria-label="上一张">${Icon.ui('chevron-left')}</button>
            <img id="photo-image" alt="">
            <button class="photos-arrow next" data-photo="next" aria-label="下一张">${Icon.ui('chevron-right')}</button>
          </div>
          <div class="photos-bottom">
            <button class="secondary-btn" data-photo="setwall">${Icon.ui('display')}&nbsp; 设为桌面背景</button>
          </div>
        </div>
      </div>
    `;
    const rootEl = win.body.firstElementChild;
    const gallery = rootEl.querySelector('#photos-gallery');
    const viewer = rootEl.querySelector('#photos-viewer');
    const photoImg = rootEl.querySelector('#photo-image');
    const photoTitle = rootEl.querySelector('#photo-title');
    const photoCount = rootEl.querySelector('#photo-count');

    function renderGallery() {
      if (!images.length) {
        gallery.innerHTML = `<div class="photos-empty" style="grid-column:1/-1">${Icon.ui('picture')}<div>还没有照片</div><div class="tiny-meta">在文件资源管理器中打开「图片」文件夹看看。</div></div>`;
        return;
      }
      gallery.innerHTML = images.map((n, i) => `
        <button class="photo-card" data-photo-index="${i}">
          <div class="photo-img-wrap"><img src="${n.image}" alt="${U.escapeHtml(n.name)}" loading="lazy"></div>
          <div class="photo-meta">${U.escapeHtml(n.name)}<div class="photo-sub">${U.escapeHtml(String(n.modified || ''))} · ${U.escapeHtml(n.size || '')}</div></div>
        </button>`).join('');
    }

    function openViewer(index) {
      if (!images.length) return;
      viewerIndex = (index + images.length) % images.length;
      const node = images[viewerIndex];
      photoImg.src = node.image;
      photoImg.alt = node.name;
      photoTitle.textContent = node.name;
      photoCount.textContent = `${viewerIndex + 1} / ${images.length}`;
      viewer.hidden = false;
      win.setTitle(`${node.name} - 照片`);
      const wall = Win11.wallpapers.find(w => w.file === node.image);
      const setBtn = viewer.querySelector('[data-photo="setwall"]');
      setBtn.innerHTML = wall ? `${Icon.ui('display')}&nbsp; 设为桌面背景` : `${Icon.ui('display')}&nbsp; 图片文件`;
      setBtn.disabled = !wall;
      setBtn.style.opacity = wall ? '1' : '.5';
    }

    function closeViewer() {
      viewer.hidden = true;
      viewerIndex = -1;
      win.setTitle('照片');
    }

    rootEl.addEventListener('click', e => {
      const card = e.target.closest('[data-photo-index]');
      if (card) openViewer(Number(card.dataset.photoIndex));
      const btn = e.target.closest('[data-photo]');
      if (!btn) return;
      const act = btn.dataset.photo;
      if (act === 'close') closeViewer();
      if (act === 'prev') openViewer(viewerIndex - 1);
      if (act === 'next') openViewer(viewerIndex + 1);
      if (act === 'setwall') {
        const node = images[viewerIndex];
        const wall = node && Win11.wallpapers.find(w => w.file === node.image);
        if (wall) {
          Win11.Store.set('wallpaper', wall.id);
          Win11.ui.toast('桌面背景已更新', `现在使用「${wall.name}」。`, { icon: Icon.app('photos') });
        }
      }
    });

    const onKey = e => {
      if (!win.isFocused() || viewer.hidden) return;
      if (e.key === 'Escape') { closeViewer(); e.preventDefault(); }
      if (e.key === 'ArrowLeft') { openViewer(viewerIndex - 1); e.preventDefault(); }
      if (e.key === 'ArrowRight') { openViewer(viewerIndex + 1); e.preventDefault(); }
    };
    document.addEventListener('keydown', onKey);
    win.addCleanup(() => document.removeEventListener('keydown', onKey));

    win.update = opts2 => {
      if (!opts2 || !opts2.fileId) return;
      const node = fs.find(opts2.fileId);
      if (!node) return;
      let idx = images.findIndex(n => n.image === node.image || n.id === node.id);
      if (idx < 0) { images = [node, ...images]; idx = 0; renderGallery(); }
      openViewer(idx);
    };

    win.setTitle('照片');
    renderGallery();
    if (opts && opts.fileId) {
      const node = fs.find(opts.fileId);
      const idx = images.findIndex(n => n.id === node.id || n.image === (node && node.image));
      if (idx >= 0) openViewer(idx);
    }
  };

  /* ================================ Weather =============================== */
  apps.weather = function (win) {
    const w = Win11.weather;
    const iconFor = name => ({ sun: 'sun', cloud: 'cloud', rain: 'rain', moon: 'moon', wind: 'wind' }[name] || 'sun');
    win.setTitle('天气');
    const now = new Date();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    win.body.innerHTML = `
      <div class="weather-app">
        <div class="wa-inner">
          <div class="wa-head">
            <div>
              <div class="wa-location">${Icon.ui('location')}<span>${w.city} · 朝阳区</span></div>
              <div class="wa-updated">更新于 ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')} · ${month} 月 ${day} 日</div>
            </div>
            <div class="wa-switch">
              <button class="active">今天</button>
              <button>每小时</button>
              <button>10 天</button>
            </div>
          </div>
          <div class="wa-hero">
            <div class="wa-hero-main">
              <div class="wa-temp">${w.temp}°</div>
              <div class="wa-cond">${w.condition} · 体感 ${w.feels}°</div>
              <div class="wa-range">最高 ${w.high}° · 最低 ${w.low}°</div>
            </div>
            ${Icon.ui('sun', 'wa-big-icon')}
          </div>
          <div class="wa-stats">
            <div class="wa-stat"><div class="wa-stat-label">${Icon.ui('wind')}风速</div><div class="wa-stat-value">${w.wind}</div></div>
            <div class="wa-stat"><div class="wa-stat-label">${Icon.ui('rain')}湿度</div><div class="wa-stat-value">${w.humidity}%</div></div>
            <div class="wa-stat"><div class="wa-stat-label">${Icon.ui('sun')}紫外线</div><div class="wa-stat-value">低 · 指数 2</div></div>
          </div>
          <div class="wa-section-title">每小时预报</div>
          <div class="wa-hours">
            ${w.hourly.map(h => `<div class="wa-hour"><span>${h.t}</span>${Icon.ui(iconFor(h.icon))}<strong>${h.deg}°</strong></div>`).join('')}
          </div>
          <div class="wa-section-title">未来 10 天</div>
          <div class="wa-daily">
            ${w.daily.map(d => `<div class="wa-day"><span>${d.day}</span>${Icon.ui(iconFor(d.icon))}<span class="low">${d.low}°</span><span class="wa-temp-bar"><i style="width:${Math.round(20 + (d.high - 14) * 9)}%"></i></span><span class="hi">${d.high}°</span></div>`).join('')}
          </div>
          <div class="wa-section-title">空气质量</div>
          <div class="wa-stat" style="margin-bottom:4px"><div class="wa-stat-label">AQI 42 · 优</div><div class="wa-stat-value">空气清新，适合户外散步。</div></div>
        </div>
      </div>
    `;
    const tabs = win.body.querySelectorAll('.wa-switch button');
    tabs.forEach(btn => btn.addEventListener('click', () => {
      tabs.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    }));
  };

  /* ================================ Recycle =============================== */
  apps.recycle = function (win) {
    win.setTitle('回收站');
    win.body.innerHTML = `
      <div class="recycle-app">
        <div class="recycle-toolbar">
          <button class="ex-cmd" data-act="restore-all">${Icon.ui('restore')}<span>还原所有项目</span></button>
          <button class="ex-cmd" data-act="empty">${Icon.ui('trash')}<span>清空回收站</span></button>
          <span class="grow"></span>
          <span class="tiny-meta" id="recycle-count"></span>
        </div>
        <div class="recycle-body" id="recycle-body"></div>
      </div>
    `;
    const rootEl = win.body.firstElementChild;
    const body = rootEl.querySelector('#recycle-body');
    const countEl = rootEl.querySelector('#recycle-count');
    const toolbarButtons = rootEl.querySelectorAll('[data-act]');

    function restoreItem(item) {
      const parent = fs.find(item.parentId) || fs.find('desktop');
      const node = item.node;
      fs.add(node, parent);
      const idx = Win11.recycle.indexOf(item);
      if (idx >= 0) Win11.recycle.splice(idx, 1);
      refreshDesktop();
    }

    function render() {
      const items = Win11.recycle;
      countEl.textContent = `${items.length} 个项目`;
      toolbarButtons.forEach(btn => { btn.disabled = !items.length; btn.style.opacity = items.length ? '1' : '.4'; });
      if (!items.length) {
        body.innerHTML = `<div class="recycle-empty">
          ${Icon.app('recycle')}
          <div>回收站是空的</div>
          <div class="recycle-note">从文件资源管理器删除的项目会先移动到这里。你可以在演示中尝试删除桌面上的文件。</div>
        </div>`;
        return;
      }
      const groups = {};
      items.forEach(it => { (groups[it.parentName] = groups[it.parentName] || []).push(it); });
      body.innerHTML = Object.keys(groups).map(loc => `
        <div class="recycle-location">原位置：${U.escapeHtml(loc)}</div>
        ${groups[loc].map(it => `
          <div class="recycle-row" data-recycle="${U.escapeHtml(it.node.id)}">
            ${folderIcon(it.node)}
            <span class="recycle-row-main">
              <span class="recycle-name">${U.escapeHtml(it.node.name)}</span>
              <span class="recycle-sub">删除于 ${U.escapeHtml(U.formatDateTime(it.at))} · ${fileTypeText(it.node)}</span>
            </span>
            <button class="secondary-btn" data-restore="${U.escapeHtml(it.node.id)}">还原</button>
          </div>`).join('')}`).join('');
    }

    body.addEventListener('click', e => {
      const restoreBtn = e.target.closest('[data-restore]');
      if (restoreBtn) {
        const item = Win11.recycle.find(it => it.node.id === restoreBtn.dataset.restore);
        if (item) {
          const name = item.node.name;
          restoreItem(item);
          render();
          Win11.ui.toast('已还原', `${name} 已还原到原位置。`, { icon: Icon.app('recycle') });
        }
      }
    });

    rootEl.querySelector('[data-act="restore-all"]').addEventListener('click', () => {
      if (!Win11.recycle.length) return;
      const items = Win11.recycle.slice();
      items.forEach(restoreItem);
      Win11.recycle.length = 0;
      render();
      Win11.ui.toast('已还原所有项目', `${items.length} 个项目已回到原位置。`, { icon: Icon.app('recycle') });
    });
    rootEl.querySelector('[data-act="empty"]').addEventListener('click', () => {
      if (!Win11.recycle.length) return;
      Win11.ui.confirm({
        title: '要永久删除这些项目吗？',
        message: `回收站中有 ${Win11.recycle.length} 个项目。永久删除后无法恢复。`,
        okText: '删除',
        danger: true,
        onOk: () => {
          Win11.recycle.length = 0;
          render();
          Win11.ui.toast('回收站已清空', '所有项目已被永久删除。', { icon: Icon.app('recycle') });
        }
      });
    });

    Win11.refreshRecycleWindows = render;
    win.addCleanup(() => {
      if (Win11.refreshRecycleWindows === render) Win11.refreshRecycleWindows = null;
    });

    render();
  };

  Win11.apps = apps;
})();
