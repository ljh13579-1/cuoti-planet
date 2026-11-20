/* Shared state, virtual file system and app metadata. */
(function () {
  const Win11 = (window.Win11 = window.Win11 || {});

  const SETTINGS_KEY = 'win11web.settings.v1';
  const FILES_KEY = 'win11web.files.v1';

  const defaults = {
    theme: 'light',
    wallpaper: 'bloom',
    accent: '#0067c0',
    align: 'center',
    userName: 'Aki',
    brightness: 100,
    volume: 58,
    nightLight: false,
    wifi: true,
    bluetooth: true,
    airplane: false,
    saver: false,
    dnd: false
  };

  function hexToRgba(hex, alpha) {
    const h = hex.replace('#', '');
    const n = h.length === 3 ? h.split('').map(c => c + c).join('') : h;
    const num = parseInt(n, 16);
    return `rgba(${(num >> 16) & 255}, ${(num >> 8) & 255}, ${num & 255}, ${alpha})`;
  }

  function shade(hex, percent) {
    const h = hex.replace('#', '');
    const n = h.length === 3 ? h.split('').map(c => c + c).join('') : h;
    let num = parseInt(n, 16);
    let r = (num >> 16) & 255, g = (num >> 8) & 255, b = num & 255;
    const t = percent < 0 ? 0 : 255;
    const p = Math.abs(percent) / 100;
    r = Math.round((t - r) * p + r);
    g = Math.round((t - g) * p + g);
    b = Math.round((t - b) * p + b);
    return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
  }

  const wallpapers = [
    { id: 'bloom', name: '绽放', file: 'assets/wallpaper-bloom.svg' },
    { id: 'dawn', name: '晨曦', file: 'assets/wallpaper-dawn.svg' },
    { id: 'lagoon', name: '泻湖', file: 'assets/wallpaper-lagoon.svg' },
    { id: 'graphite', name: '石墨', file: 'assets/wallpaper-graphite.svg' }
  ];

  const accents = [
    { value: '#0067c0', name: '经典蓝' },
    { value: '#008272', name: '青绿' },
    { value: '#4f6bed', name: '靛蓝' },
    { value: '#8764b8', name: '薰衣草' },
    { value: '#c239b3', name: '洋红' },
    { value: '#ca5010', name: '琥珀' },
    { value: '#107c10', name: '绿色' },
    { value: '#c42b1c', name: '红' }
  ];

  const Store = {
    state: { ...defaults },
    load() {
      try {
        const raw = localStorage.getItem(SETTINGS_KEY);
        if (raw) this.state = { ...defaults, ...JSON.parse(raw) };
      } catch (e) { /* private mode or file restrictions */ }
      this.apply();
    },
    persist() {
      try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(this.state)); } catch (e) {}
    },
    set(key, value) {
      if (this.state[key] === value) return;
      this.state[key] = value;
      this.persist();
      this.apply(key);
      document.dispatchEvent(new CustomEvent('win11:settings', { detail: { key, value } }));
    },
    patch(values) {
      let changed = false;
      Object.entries(values).forEach(([k, v]) => {
        if (this.state[k] !== v) { this.state[k] = v; changed = true; }
      });
      if (changed) {
        this.persist();
        this.apply();
        document.dispatchEvent(new CustomEvent('win11:settings', { detail: {} }));
      }
    },
    apply(changedKey) {
      const s = this.state;
      const root = document.documentElement;
      root.dataset.theme = s.theme;
      root.dataset.wallpaper = s.wallpaper;
      root.style.setProperty('--accent', s.accent);
      root.style.setProperty('--accent-hover', shade(s.accent, s.theme === 'dark' ? 12 : -14));
      root.style.setProperty('--accent-soft', hexToRgba(s.accent, s.theme === 'dark' ? 0.26 : 0.14));
      root.style.setProperty('--accent-soft-2', hexToRgba(s.accent, s.theme === 'dark' ? 0.4 : 0.22));
      document.body.classList.toggle('taskbar-left', s.align === 'left');
      if (s.airplane) {
        // Airplane mode is reflected in the quick settings UI, not persisted as a second switch.
      }
      if (changedKey) document.body.dataset.lastSetting = changedKey;
    },
    reset() {
      this.state = { ...defaults };
      this.persist();
      this.apply();
      document.dispatchEvent(new CustomEvent('win11:settings', { detail: { reset: true } }));
    }
  };

  /* ----------------------------- virtual files ----------------------------- */
  const welcome = `欢迎使用 Windows 11 网页版
==============================

这是一个完全离线运行的桌面模拟页面。

可以试试这些操作：
• 双击桌面图标，或从「开始」菜单打开应用
• 拖动窗口标题栏移动窗口，拖到屏幕边缘可以贴靠
• 悬停窗口右上角的「最大化」按钮，选择贴靠布局
• 右键点击桌面，可以更换壁纸或新建文本文档
• 在「设置 → 个性化」里切换主题、强调色和壁纸

所有图标与壁纸都是为本示例自行绘制的，
仅用于个人学习和演示。祝你玩得开心！
`;

  const plan = `# 项目计划

## 本周目标
- [x] 整理桌面图标与虚拟文件系统
- [x] 完成窗口拖拽与边缘贴靠
- [ ] 给「天气」加上未来十天的预报
- [ ] 把常用文件夹固定到「快速访问」

## 备注
真正好用的桌面，不需要很多功能，只需要每次点开都顺手。
`;

  const notes = `灵感记录
--------
1. 给窗口标题栏加一条很淡的 Mica 质感。
2. 贴靠布局的预览要用强调色，别用灰色。
3. 深色模式下，任务栏的模糊可以再重一点。
4. 锁屏的时钟不要秒针，安静一点。
`;

  const meeting = `周会纪要 · 11 月 12 日
---------------------
参与：Aki、Lin、Momo

1. 本周完成桌面外壳与开始菜单。
2. 文件资源管理器先支持主页、桌面、文档、图片。
3. 下次演示前补上天气与照片应用。
4. 所有数据保存在浏览器本地，不上传网络。

下次同步：周五 16:00
`;

  const travel = `周末散步清单

▢ 带上相机
▢ 去河边看日落
▢ 买热美式
▢ 回家把照片设成壁纸

天气好的话，走路去就好。
`;

  const tips = `Windows 11 网页版 · 使用提示
----------------------------
· 右上角的最大化按钮悬停后会出现贴靠布局。
· 桌面右键可以新建文件夹或文本文档。
· Ctrl + Alt + T 打开终端。
· Ctrl + K 打开搜索。
· 设置里的改动会自动保存在浏览器中。
`;

  const images = [
    { id: 'pic-bloom', name: '绽放.svg', image: 'assets/wallpaper-bloom.svg', size: '38 KB', modified: '2026/11/10 18:02' },
    { id: 'pic-dawn', name: '晨曦.svg', image: 'assets/wallpaper-dawn.svg', size: '41 KB', modified: '2026/11/10 18:04' },
    { id: 'pic-lagoon', name: '泻湖.svg', image: 'assets/wallpaper-lagoon.svg', size: '36 KB', modified: '2026/11/10 18:06' },
    { id: 'pic-graphite', name: '石墨.svg', image: 'assets/wallpaper-graphite.svg', size: '33 KB', modified: '2026/11/10 18:08' }
  ];

  const root = {
    id: 'thispc', name: '此电脑', type: 'folder', modified: '2026/11/13 09:00',
    children: [
      {
        id: 'desktop', name: '桌面', type: 'folder', modified: '2026/11/13 09:20',
        children: [
          {
            id: 'welcome', name: '欢迎使用.txt', type: 'file', kind: 'text', size: '1.4 KB',
            modified: '2026/11/13 09:20', content: welcome
          },
          {
            id: 'projects', name: '项目资料', type: 'folder', modified: '2026/11/12 21:15',
            children: [
              { id: 'plan', name: '项目计划.md', type: 'file', kind: 'text', size: '1.1 KB', modified: '2026/11/12 21:15', content: plan },
              { id: 'notes', name: '灵感记录.txt', type: 'file', kind: 'text', size: '0.6 KB', modified: '2026/11/11 22:40', content: notes }
            ]
          },
          {
            id: 'desktop-bloom', name: '绽放.svg', type: 'file', kind: 'image', image: 'assets/wallpaper-bloom.svg',
            size: '38 KB', modified: '2026/11/10 18:02'
          }
        ]
      },
      {
        id: 'docs', name: '文档', type: 'folder', modified: '2026/11/12 17:30',
        children: [
          { id: 'meeting', name: '周会纪要.txt', type: 'file', kind: 'text', size: '0.9 KB', modified: '2026/11/12 17:30', content: meeting },
          { id: 'travel', name: '周末散步.txt', type: 'file', kind: 'text', size: '0.4 KB', modified: '2026/11/09 08:12', content: travel }
        ]
      },
      {
        id: 'downloads', name: '下载', type: 'folder', modified: '2026/11/08 14:02',
        children: [
          { id: 'tips', name: '使用提示.txt', type: 'file', kind: 'text', size: '0.7 KB', modified: '2026/11/08 14:02', content: tips }
        ]
      },
      {
        id: 'pictures', name: '图片', type: 'folder', modified: '2026/11/10 18:08',
        children: images.map(img => ({
          id: img.id, name: img.name, type: 'file', kind: 'image', image: img.image,
          size: img.size, modified: img.modified
        }))
      },
      { id: 'music', name: '音乐', type: 'folder', modified: '2026/10/28 10:00', children: [] },
      { id: 'videos', name: '视频', type: 'folder', modified: '2026/10/28 10:00', children: [] },
      {
        id: 'c-drive', name: '本地磁盘 (C:)', type: 'drive', modified: '2026/11/13 08:00',
        children: [
          {
            id: 'users-folder', name: '用户', type: 'folder', modified: '2026/11/13 08:00',
            children: [
              {
                id: 'aki-folder', name: 'Aki', type: 'folder', modified: '2026/11/13 08:00',
                children: [
                  { id: 'readme-c', name: 'readme.txt', type: 'file', kind: 'text', size: '0.3 KB', modified: '2026/11/01 09:00', content: '这里是 C:\\Users\\Aki。\n不要删除系统文件。' }
                ]
              }
            ]
          },
          {
            id: 'windows-folder', name: 'Windows', type: 'folder', modified: '2026/10/01 12:00',
            children: [
              { id: 'sysinfo', name: '系统信息.txt', type: 'file', kind: 'text', size: '0.5 KB', modified: '2026/11/13 08:00', content: 'Windows 11 专业版\n版本 24H2\n内部版本 26100.2314' }
            ]
          }
        ]
      }
    ]
  };

  const nodeIndex = new Map();
  function indexTree(node, parent) {
    node._parent = parent || null;
    nodeIndex.set(node.id, node);
    (node.children || []).forEach(child => indexTree(child, node));
  }
  indexTree(root, null);

  let overrides = {};
  try { overrides = JSON.parse(localStorage.getItem(FILES_KEY) || '{}'); } catch (e) {}
  Object.keys(overrides).forEach(id => {
    const node = nodeIndex.get(id);
    if (node && node.type === 'file') node.content = overrides[id];
  });

  const fs = {
    root,
    find(id) { return nodeIndex.get(id) || null; },
    parentOf(node) { return node ? node._parent : null; },
    pathOf(node) {
      const path = [];
      let cur = node;
      while (cur) { path.unshift(cur); cur = cur._parent; }
      return path;
    },
    add(node, parent) {
      if (!node || !parent) return null;
      node.id = node.id || `node-${Date.now()}-${Math.floor(Math.random() * 999)}`;
      const index = (n, p) => {
        n._parent = p;
        nodeIndex.set(n.id, n);
        (n.children || []).forEach(child => index(child, n));
      };
      index(node, parent);
      parent.children.push(node);
      return node;
    },
    remove(node) {
      const parent = node._parent;
      if (!parent) return false;
      const i = parent.children.indexOf(node);
      if (i >= 0) parent.children.splice(i, 1);
      const unindex = n => { nodeIndex.delete(n.id); (n.children || []).forEach(unindex); };
      unindex(node);
      return true;
    },
    saveText(node, text) {
      node.content = text;
      overrides[node.id] = text;
      try { localStorage.setItem(FILES_KEY, JSON.stringify(overrides)); } catch (e) {}
    },
    createText(name, text, parent) {
      return this.add({
        name, type: 'file', kind: 'text', size: '0.1 KB',
        modified: formatDateTime(new Date()), content: text || ''
      }, parent || fs.find('desktop'));
    },
    createFolder(name, parent) {
      return this.add({
        name, type: 'folder', modified: formatDateTime(new Date()), children: []
      }, parent || fs.find('desktop'));
    }
  };

  function pad(n) { return String(n).padStart(2, '0'); }
  function formatDateTime(d) {
    return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  const appsMeta = {
    explorer: {
      name: '文件资源管理器', icon: 'explorer', width: 1040, height: 660, minWidth: 640, minHeight: 420,
      pinned: true, taskbar: true, keywords: ['文件', '文件夹', '资源', 'explorer', 'files']
    },
    notepad: {
      name: '记事本', icon: 'notepad', width: 840, height: 580, minWidth: 480, minHeight: 340,
      pinned: true, taskbar: true, keywords: ['记事本', '文本', '笔记', 'notepad', 'txt']
    },
    calculator: {
      name: '计算器', icon: 'calculator', width: 420, height: 560, minWidth: 340, minHeight: 470,
      single: true, pinned: true, taskbar: true, keywords: ['计算器', '计算', 'calculator']
    },
    terminal: {
      name: '终端', icon: 'terminal', width: 820, height: 520, minWidth: 480, minHeight: 320,
      pinned: true, taskbar: true, keywords: ['终端', '命令行', 'powershell', 'terminal', 'cmd']
    },
    photos: {
      name: '照片', icon: 'photos', width: 960, height: 660, minWidth: 620, minHeight: 420,
      single: true, pinned: true, taskbar: true, keywords: ['照片', '图片', '壁纸', 'photos', 'images']
    },
    settings: {
      name: '设置', icon: 'settings', width: 1000, height: 680, minWidth: 720, minHeight: 500,
      single: true, pinned: true, taskbar: true, keywords: ['设置', '系统', '个性化', 'settings']
    },
    weather: {
      name: '天气', icon: 'weather', width: 880, height: 640, minWidth: 600, minHeight: 460,
      single: true, pinned: true, taskbar: false, keywords: ['天气', '气温', '预报', 'weather']
    },
    recycle: {
      name: '回收站', icon: 'recycle', width: 760, height: 540, minWidth: 520, minHeight: 360,
      single: true, pinned: true, taskbar: false, keywords: ['回收站', '删除', 'recycle', 'trash']
    }
  };

  const startOrder = ['explorer', 'notepad', 'calculator', 'terminal', 'settings', 'photos', 'weather', 'recycle'];
  const taskbarOrder = ['explorer', 'notepad', 'terminal', 'calculator', 'photos', 'settings'];

  const recommended = [
    { fileId: 'welcome' },
    { fileId: 'plan' },
    { fileId: 'pic-bloom' },
    { fileId: 'meeting' }
  ];

  const weather = {
    city: '北京', condition: '晴', temp: 22, high: 25, low: 14, feels: 23, humidity: 38, wind: '西北风 2 级',
    hourly: [
      { t: '现在', deg: 22, icon: 'sun' },
      { t: '14:00', deg: 24, icon: 'sun' },
      { t: '15:00', deg: 25, icon: 'sun' },
      { t: '16:00', deg: 24, icon: 'cloud' },
      { t: '17:00', deg: 22, icon: 'cloud' },
      { t: '18:00', deg: 20, icon: 'cloud' },
      { t: '19:00', deg: 18, icon: 'moon' },
      { t: '20:00', deg: 17, icon: 'moon' }
    ],
    daily: [
      { day: '今天', icon: 'sun', high: 25, low: 14 },
      { day: '周六', icon: 'sun', high: 24, low: 13 },
      { day: '周日', icon: 'cloud', high: 21, low: 12 },
      { day: '周一', icon: 'rain', high: 18, low: 10 },
      { day: '周二', icon: 'cloud', high: 19, low: 11 },
      { day: '周三', icon: 'sun', high: 23, low: 12 },
      { day: '周四', icon: 'sun', high: 24, low: 13 },
      { day: '周五', icon: 'wind', high: 22, low: 12 }
    ]
  };

  const notifications = [
    {
      id: 'n1', appName: 'Windows 安全中心', icon: { type: 'ui', name: 'shield' },
      title: '你的设备已受到保护', body: '上次扫描：今天 09:12，未发现威胁。', time: '刚刚'
    },
    {
      id: 'n2', appName: '照片', icon: { type: 'app', name: 'photos' },
      title: '新的回忆已准备好', body: '「绽放」与「晨曦」适合作为今天的桌面背景。', time: '12 分钟前'
    },
    {
      id: 'n3', appName: '系统', icon: { type: 'ui', name: 'info' },
      title: '欢迎回来', body: '这是为个人使用而制作的 Windows 11 风格网页桌面。', time: '1 小时前'
    }
  ];

  let userName = Store.state.userName || 'Aki';

  Win11.defaults = defaults;
  Win11.Store = Store;
  Win11.wallpapers = wallpapers;
  Win11.accents = accents;
  Win11.fs = fs;
  Win11.recycle = [];
  Win11.appsMeta = appsMeta;
  Win11.startOrder = startOrder;
  Win11.taskbarOrder = taskbarOrder;
  Win11.recommended = recommended;
  Win11.weather = weather;
  Win11.notifications = notifications;
  Win11.wallpaperOf = id => wallpapers.find(w => w.id === id) || wallpapers[0];
  Win11.initialOf = name => (name || Store.state.userName || 'Aki').trim().charAt(0).toUpperCase();
  Win11.userName = () => Store.state.userName || 'Aki';

  Win11.util = {
    $: (sel, root) => (root || document).querySelector(sel),
    $$: (sel, root) => Array.from((root || document).querySelectorAll(sel)),
    pad,
    clamp: (v, a, b) => Math.max(a, Math.min(b, v)),
    escapeHtml: s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])),
    formatDateTime,
    hexToRgba,
    uid: prefix => `${prefix || 'id'}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`,
    formatSize(size) {
      if (typeof size === 'string') return size;
      if (size == null) return '—';
      if (size < 1024) return `${size} B`;
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
      return `${(size / 1024 / 1024).toFixed(1)} MB`;
    }
  };
})();
