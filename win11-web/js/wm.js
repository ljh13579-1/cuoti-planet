/* Lightweight Windows 11 style window manager. */
(function () {
  const Win11 = window.Win11;
  const U = Win11.util;
  const layer = document.getElementById('windows-layer');
  const desktop = document.getElementById('desktop');
  const snapPreview = document.getElementById('snap-preview');
  const snapFlyout = document.getElementById('snap-flyout');

  const wins = [];
  let seq = 0;
  let zTop = 100;
  let activeId = null;
  let cascade = 0;
  let snapHideTimer = 0;
  let snapWin = null;

  function desktopSize() {
    const r = desktop.getBoundingClientRect();
    return { w: r.width, h: r.height };
  }

  function dispatch(event) {
    window.dispatchEvent(new CustomEvent(event || 'win11:windows'));
  }

  function activeWindow() {
    return wins.find(w => w.id === activeId && !w.closed && !w.minimized) || null;
  }

  function applyRect(win, r) {
    const rr = {
      x: Math.round(r.x), y: Math.round(r.y),
      w: Math.max(1, Math.round(r.w)), h: Math.max(1, Math.round(r.h))
    };
    win.rect = rr;
    win.el.style.left = rr.x + 'px';
    win.el.style.top = rr.y + 'px';
    win.el.style.width = rr.w + 'px';
    win.el.style.height = rr.h + 'px';
  }

  function zoneRect(zone) {
    const { w: dw, h: dh } = desktopSize();
    const g = 6;
    const halfW = Math.floor((dw - g * 3) / 2);
    const halfH = Math.floor((dh - g * 3) / 2);
    const thirdW = Math.floor((dw - g * 4) / 3);
    switch (zone) {
      case 'max': return { x: 0, y: 0, w: dw, h: dh };
      case 'left': return { x: g, y: g, w: halfW, h: dh - g * 2 };
      case 'right': return { x: dw - g - halfW, y: g, w: halfW, h: dh - g * 2 };
      case 'tl': return { x: g, y: g, w: halfW, h: halfH };
      case 'tr': return { x: dw - g - halfW, y: g, w: halfW, h: halfH };
      case 'bl': return { x: g, y: dh - g - halfH, w: halfW, h: halfH };
      case 'br': return { x: dw - g - halfW, y: dh - g - halfH, w: halfW, h: halfH };
      case 'c1': return { x: g, y: g, w: thirdW, h: dh - g * 2 };
      case 'c2': return { x: g * 2 + thirdW, y: g, w: thirdW, h: dh - g * 2 };
      case 'c3': return { x: g * 3 + thirdW * 2, y: g, w: thirdW, h: dh - g * 2 };
      default: return null;
    }
  }

  function updateMaxGlyph(win) {
    const btn = win.el.querySelector('[data-win="max"]');
    if (!btn) return;
    btn.innerHTML = Win11.Icon.ui(win.maximized ? 'restore' : 'maximize');
    btn.title = win.maximized ? '还原' : '最大化';
    btn.setAttribute('aria-label', win.maximized ? '还原' : '最大化');
  }

  function setActive(win) {
    activeId = win ? win.id : null;
    wins.forEach(w => {
      const on = win && w.id === win.id;
      w.active = on;
      w.el.classList.toggle('active', on);
      w.el.classList.toggle('inactive', !on);
    });
    dispatch();
  }

  function focus(win, opts) {
    if (!win || win.closed) return;
    if (win.minimized) restore(win, true);
    win.el.classList.remove('inactive');
    win.el.classList.add('active');
    win.active = true;
    win.el.style.zIndex = ++zTop;
    win.z = zTop;
    activeId = win.id;
    wins.forEach(w => {
      if (w !== win) {
        w.active = false;
        w.el.classList.remove('active');
        w.el.classList.add('inactive');
      }
    });
    if (!opts || !opts.silent) dispatch();
  }

  function blurAll() {
    activeId = null;
    wins.forEach(w => {
      w.active = false;
      w.el.classList.remove('active');
      w.el.classList.add('inactive');
    });
    dispatch();
  }

  function restore(win, silent) {
    if (!win || win.closed) return;
    win.minimized = false;
    win.el.hidden = false;
    win.el.style.zIndex = ++zTop;
    win.z = zTop;
    if (!silent) focus(win);
  }

  function minimize(win) {
    if (!win || win.closed || win.minimized) return;
    win.minimized = true;
    win.el.classList.add('minimizing');
    const done = () => {
      win.el.classList.remove('minimizing');
      if (win.minimized) win.el.hidden = true;
    };
    setTimeout(done, 150);
    if (activeId === win.id) {
      const next = wins.filter(w => !w.closed && !w.minimized && w !== win).sort((a, b) => b.z - a.z)[0];
      if (next) focus(next); else blurAll();
    } else {
      dispatch();
    }
  }

  function maximize(win) {
    if (!win || win.closed) return;
    const { w: dw, h: dh } = desktopSize();
    if (win.maximized) {
      const r = win.restoreRect || { x: 60, y: 40, w: Math.min(960, dw - 80), h: Math.min(640, dh - 80) };
      win.maximized = false;
      win.el.classList.remove('maximized');
      applyRect(win, r);
    } else {
      win.restoreRect = win.rect;
      win.maximized = true;
      win.snapped = null;
      win.el.classList.add('maximized');
      applyRect(win, { x: 0, y: 0, w: dw, h: dh });
    }
    updateMaxGlyph(win);
    focus(win);
  }

  function snap(win, zone) {
    if (!win || win.closed) return;
    if (zone === 'max') { maximize(win); return; }
    const r = zoneRect(zone);
    if (!r) return;
    if (!win.snapped && !win.maximized) win.restoreRect = win.rect;
    win.snapped = zone;
    win.maximized = false;
    win.el.classList.remove('maximized');
    win.restoreRect = win.restoreRect || win.rect;
    applyRect(win, r);
    updateMaxGlyph(win);
    focus(win);
  }

  function close(win) {
    if (!win || win.closed) return;
    win.closed = true;
    hideSnapFlyout();
    try { (win.cleanups || []).forEach(fn => fn()); } catch (e) { console.warn(e); }
    win.el.classList.add('closing');
    const wasActive = activeId === win.id;
    setTimeout(() => {
      win.el.remove();
      const i = wins.indexOf(win);
      if (i >= 0) wins.splice(i, 1);
      if (wasActive) {
        const next = wins.filter(w => !w.closed && !w.minimized).sort((a, b) => b.z - a.z)[0];
        if (next) focus(next); else { activeId = null; dispatch(); }
      } else {
        dispatch();
      }
    }, 145);
  }

  function open(appId, opts) {
    const meta = Win11.appsMeta[appId];
    if (!meta) return null;
    const existing = wins.find(w => w.appId === appId && !w.closed);
    if (existing && meta.single) {
      if (existing.minimized) restore(existing);
      focus(existing);
      if (typeof existing.update === 'function') existing.update(opts || {});
      return existing;
    }

    const { w: dw, h: dh } = desktopSize();
    const offset = (cascade++ % 6) * 26 - 52;
    const availW = Math.max(220, dw - 16);
    const availH = Math.max(180, dh - 16);
    const minW = Math.min(meta.minWidth || 300, availW);
    const minH = Math.min(meta.minHeight || 200, availH);
    const maxW = Math.max(minW, dw - 36);
    const maxH = Math.max(minH, dh - 36);
    const width = Math.min(Math.max(meta.width || 860, minW), maxW);
    const height = Math.min(Math.max(meta.height || 560, minH), maxH);
    const x = U.clamp(Math.round((dw - width) / 2 + offset), 8, Math.max(8, dw - width - 8));
    const y = U.clamp(Math.round((dh - height) / 2 + offset * .65), 8, Math.max(8, dh - height - 8));

    const id = 'win-' + (++seq);
    const el = document.createElement('section');
    el.className = 'window active';
    el.id = id;
    el.setAttribute('role', 'dialog');
    el.setAttribute('aria-label', meta.name);
    el.style.width = width + 'px';
    el.style.height = height + 'px';
    el.style.left = x + 'px';
    el.style.top = y + 'px';
    el.style.minWidth = minW + 'px';
    el.style.minHeight = minH + 'px';
    el.style.zIndex = ++zTop;
    el.innerHTML = `
      <div class="window-titlebar">
        <div class="win-appicon">${Win11.Icon.app(meta.icon)}</div>
        <div class="win-title">${U.escapeHtml(meta.name)}</div>
        <div class="window-controls">
          <button class="win-ctrl" data-win="min" title="最小化" aria-label="最小化">${Win11.Icon.ui('minus')}</button>
          <div class="snap-anchor">
            <button class="win-ctrl" data-win="max" title="最大化" aria-label="最大化">${Win11.Icon.ui('maximize')}</button>
          </div>
          <button class="win-ctrl close" data-win="close" title="关闭" aria-label="关闭">${Win11.Icon.ui('close')}</button>
        </div>
      </div>
      <div class="window-body"></div>
      <div class="rz rz-n" data-rz="n"></div><div class="rz rz-s" data-rz="s"></div>
      <div class="rz rz-w" data-rz="w"></div><div class="rz rz-e" data-rz="e"></div>
      <div class="rz rz-nw" data-rz="nw"></div><div class="rz rz-ne" data-rz="ne"></div>
      <div class="rz rz-sw" data-rz="sw"></div><div class="rz rz-se" data-rz="se"></div>
    `;
    layer.appendChild(el);

    const win = {
      id, appId, meta, el,
      titlebar: el.querySelector('.window-titlebar'),
      body: el.querySelector('.window-body'),
      rect: { x, y, w: width, h: height },
      title: meta.name,
      minimized: false,
      maximized: false,
      snapped: null,
      restoreRect: null,
      active: true,
      closed: false,
      z: zTop,
      cleanups: [],
      setTitle(text) {
        const t = String(text || meta.name);
        this.title = t;
        const node = this.el.querySelector('.win-title');
        if (node) node.textContent = t;
        this.el.setAttribute('aria-label', t);
        dispatch();
      },
      focus() { focus(win); },
      close() { close(win); },
      minimize() { minimize(win); },
      maximize() { maximize(win); },
      toggleMaximize() { maximize(win); },
      snap(zone) { snap(win, zone); },
      addCleanup(fn) { this.cleanups.push(fn); },
      isFocused() { return activeId === win.id && !win.minimized; },
      update: null
    };

    wins.push(win);
    focus(win, { silent: true });

    // Focus by clicking anywhere in the window.
    el.addEventListener('pointerdown', () => focus(win), true);

    // Window buttons.
    el.querySelectorAll('[data-win]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const act = btn.dataset.win;
        if (act === 'min') minimize(win);
        if (act === 'max') maximize(win);
        if (act === 'close') close(win);
      });
    });

    bindDrag(win);
    bindResize(win);
    bindSnapHover(win);

    if (Win11.apps && typeof Win11.apps[appId] === 'function') {
      try {
        Win11.apps[appId](win, opts || {});
      } catch (err) {
        console.error(err);
        win.body.innerHTML = `<div class="ex-empty"><span>${Win11.Icon.ui('warning')}</span><div>此应用遇到了问题，请关闭后重试。</div></div>`;
      }
    } else {
      win.body.innerHTML = `<div class="ex-empty"><span>${Win11.Icon.ui('info')}</span><div>应用尚未安装。</div></div>`;
    }

    dispatch();
    return win;
  }

  function bindDrag(win) {
    let drag = null;
    const bar = win.titlebar;

    bar.addEventListener('pointerdown', e => {
      if (e.button !== 0) return;
      if (e.target.closest('.win-ctrl')) return;
      hideSnapFlyout();
      focus(win);
      drag = {
        startX: e.clientX,
        startY: e.clientY,
        orig: { ...win.rect },
        wasBig: win.maximized || !!win.snapped,
        moved: false,
        ratioX: (e.clientX - win.rect.x) / win.rect.w,
        ratioY: (e.clientY - win.rect.y) / win.rect.h,
        zone: null
      };
      bar.setPointerCapture(e.pointerId);
      e.preventDefault();
    });

    bar.addEventListener('dblclick', e => {
      if (e.target.closest('.win-ctrl')) return;
      maximize(win);
    });

    bar.addEventListener('pointermove', e => {
      if (!drag) return;
      const dx = e.clientX - drag.startX;
      const dy = e.clientY - drag.startY;
      if (!drag.moved && Math.abs(dx) < 3 && Math.abs(dy) < 3) return;

      if (!drag.moved) {
        drag.moved = true;
        win.el.classList.add('dragging');
        document.body.classList.add('dragging-window');
        if (drag.wasBig) {
          const restored = win.restoreRect || {
            w: Math.min(win.meta.width || 900, desktopSize().w - 40),
            h: Math.min(win.meta.height || 600, desktopSize().h - 40)
          };
          win.maximized = false;
          win.snapped = null;
          win.el.classList.remove('maximized');
          win.el.style.width = Math.round(restored.w) + 'px';
          win.el.style.height = Math.round(restored.h) + 'px';
          win.rect = { ...win.rect, w: Math.round(restored.w), h: Math.round(restored.h) };
          drag.orig = { ...win.rect };
          drag.orig.x = Math.round(e.clientX - drag.ratioX * drag.orig.w);
          drag.orig.y = Math.max(0, Math.round(e.clientY - 18));
          drag.startX = e.clientX;
          drag.startY = e.clientY;
          updateMaxGlyph(win);
        }
      }

      const { w: dw, h: dh } = desktopSize();
      let nx = drag.orig.x + (e.clientX - drag.startX);
      let ny = drag.orig.y + (e.clientY - drag.startY);
      nx = U.clamp(nx, -win.rect.w + 110, dw - 110);
      ny = U.clamp(ny, 0, dh - 38);
      applyRect(win, { x: nx, y: ny, w: win.rect.w, h: win.rect.h });
      showEdgePreview(e.clientX, e.clientY, drag);
      e.preventDefault();
    });

    function endDrag(e) {
      if (!drag) return;
      const dragState = drag;
      drag = null;
      win.el.classList.remove('dragging');
      document.body.classList.remove('dragging-window');
      snapPreview.classList.remove('visible');
      if (dragState.zone) {
        snap(win, dragState.zone);
      } else if (dragState.moved) {
        // Keep a hidden restore rect in sync after a manual move.
        if (!win.snapped) win.restoreRect = null;
      }
      try { bar.releasePointerCapture(e.pointerId); } catch (err) {}
    }
    bar.addEventListener('pointerup', endDrag);
    bar.addEventListener('pointercancel', endDrag);
  }

  function showEdgePreview(px, py, drag) {
    const { w: dw } = desktopSize();
    let zone = null;
    // Only the outer few pixels trigger edge snapping, like Windows.
    if (px <= 5) zone = 'left';
    else if (px >= dw - 5) zone = 'right';
    else if (py <= 5) zone = 'max';
    if (!zone) {
      drag.zone = null;
      snapPreview.classList.remove('visible');
      return;
    }
    drag.zone = zone;
    const r = zoneRect(zone);
    snapPreview.style.left = r.x + 'px';
    snapPreview.style.top = r.y + 'px';
    snapPreview.style.width = r.w + 'px';
    snapPreview.style.height = r.h + 'px';
    snapPreview.classList.add('visible');
  }

  function bindResize(win) {
    win.el.querySelectorAll('.rz').forEach(handle => {
      handle.addEventListener('pointerdown', e => {
        if (e.button !== 0 || win.maximized) return;
        e.preventDefault();
        e.stopPropagation();
        focus(win);
        const dir = handle.dataset.rz;
        const start = { ...win.rect };
        const sx = e.clientX;
        const sy = e.clientY;
        const { w: dw, h: dh } = desktopSize();
        const minW = Math.min(win.meta.minWidth || 300, dw - 20);
        const minH = Math.min(win.meta.minHeight || 200, dh - 20);
        handle.setPointerCapture(e.pointerId);
        win.el.classList.add('dragging');
        document.body.classList.add('dragging-window');

        const move = ev => {
          const dx = ev.clientX - sx;
          const dy = ev.clientY - sy;
          let { x, y, w, h } = start;
          if (dir.includes('e')) w = U.clamp(start.w + dx, minW, dw - x);
          if (dir.includes('s')) h = U.clamp(start.h + dy, minH, dh - y);
          if (dir.includes('w')) {
            const nx = U.clamp(start.x + dx, 0, start.x + start.w - minW);
            w = start.x + start.w - nx;
            x = nx;
          }
          if (dir.includes('n')) {
            const ny = U.clamp(start.y + dy, 0, start.y + start.h - minH);
            h = start.y + start.h - ny;
            y = ny;
          }
          applyRect(win, { x, y, w, h });
          win.snapped = null;
          win.restoreRect = null;
        };
        const up = ev => {
          handle.removeEventListener('pointermove', move);
          handle.removeEventListener('pointerup', up);
          handle.removeEventListener('pointercancel', up);
          win.el.classList.remove('dragging');
          document.body.classList.remove('dragging-window');
          try { handle.releasePointerCapture(ev.pointerId); } catch (err) {}
        };
        handle.addEventListener('pointermove', move);
        handle.addEventListener('pointerup', up);
        handle.addEventListener('pointercancel', up);
      });
    });
  }

  function showSnapFlyout(win, button) {
    clearTimeout(snapHideTimer);
    snapWin = win;
    snapFlyout.hidden = false;
    const r = button.getBoundingClientRect();
    const fw = 286, fh = 300;
    const width = snapFlyout.offsetWidth || fw;
    const height = snapFlyout.offsetHeight || fh;
    let x = r.right - width;
    let y = r.bottom + 6;
    if (x < 8) x = 8;
    if (x + width > window.innerWidth - 8) x = window.innerWidth - width - 8;
    if (y + height > window.innerHeight - 8) y = Math.max(8, r.top - height - 6);
    snapFlyout.style.left = x + 'px';
    snapFlyout.style.top = y + 'px';
  }

  function hideSnapFlyout() {
    clearTimeout(snapHideTimer);
    snapWin = null;
    snapFlyout.hidden = true;
  }

  function bindSnapHover(win) {
    const anchor = win.el.querySelector('.snap-anchor');
    const btn = anchor && anchor.querySelector('[data-win="max"]');
    if (!anchor || !btn) return;
    anchor.addEventListener('pointerenter', () => {
      clearTimeout(snapHideTimer);
      showSnapFlyout(win, btn);
    });
    anchor.addEventListener('pointerleave', () => {
      snapHideTimer = setTimeout(() => {
        if (!snapFlyout.matches(':hover')) hideSnapFlyout();
      }, 260);
    });
  }

  // Snap flyout internals (one shared instance).
  snapFlyout.innerHTML = `
    <div class="snap-title">选择贴靠布局</div>
    <div class="snap-layouts">
      <div class="snap-layout" data-layout="halves">
        <button class="snap-zone" data-zone="left" aria-label="贴靠到左侧"></button>
        <button class="snap-zone" data-zone="right" aria-label="贴靠到右侧"></button>
      </div>
      <div class="snap-layout" data-layout="quads">
        <button class="snap-zone" data-zone="tl" aria-label="贴靠到左上"></button>
        <button class="snap-zone" data-zone="tr" aria-label="贴靠到右上"></button>
        <button class="snap-zone" data-zone="bl" aria-label="贴靠到左下"></button>
        <button class="snap-zone" data-zone="br" aria-label="贴靠到右下"></button>
      </div>
      <div class="snap-layout" data-layout="thirds">
        <button class="snap-zone" data-zone="c1" aria-label="贴靠到第一列"></button>
        <button class="snap-zone" data-zone="c2" aria-label="贴靠到第二列"></button>
        <button class="snap-zone" data-zone="c3" aria-label="贴靠到第三列"></button>
      </div>
    </div>
    <div class="snap-note">也可以把窗口拖到屏幕顶部或左右边缘。</div>
  `;
  snapFlyout.addEventListener('pointerenter', () => clearTimeout(snapHideTimer));
  snapFlyout.addEventListener('pointerleave', () => { snapHideTimer = setTimeout(hideSnapFlyout, 200); });
  snapFlyout.addEventListener('pointerdown', e => e.stopPropagation());
  snapFlyout.addEventListener('click', e => {
    const zoneBtn = e.target.closest('[data-zone]');
    if (!zoneBtn || !snapWin) return;
    const zone = zoneBtn.dataset.zone;
    const win = snapWin;
    hideSnapFlyout();
    snap(win, zone);
  });

  document.addEventListener('pointerdown', e => {
    if (!e.target.closest('.snap-anchor') && !e.target.closest('#snap-flyout')) hideSnapFlyout();
  });

  desktop.addEventListener('pointerdown', e => {
    if (!e.target.closest('.window')) {
      if (!e.target.closest('#taskview')) blurAll();
    }
  });

  window.addEventListener('resize', () => {
    const { w: dw, h: dh } = desktopSize();
    wins.forEach(win => {
      if (win.closed) return;
      if (win.maximized) {
        applyRect(win, { x: 0, y: 0, w: dw, h: dh });
      } else if (win.snapped) {
        const r = zoneRect(win.snapped);
        if (r) applyRect(win, r);
      } else {
        const w = Math.min(win.rect.w, dw - 16);
        const h = Math.min(win.rect.h, dh - 16);
        const x = U.clamp(win.rect.x, -w + 110, dw - 110);
        const y = U.clamp(win.rect.y, 0, dh - 38);
        applyRect(win, { x, y, w, h });
      }
    });
    hideSnapFlyout();
  });

  const WM = {
    get windows() { return wins.slice(); },
    open, close, focus, minimize, restore, maximize, snap, blurAll,
    active: activeWindow,
    byApp(appId) {
      return wins.filter(w => w.appId === appId && !w.closed).sort((a, b) => b.z - a.z);
    },
    getWindow(id) { return wins.find(w => w.id === id) || null; },
    zoneRect,
    desktopSize
  };

  Win11.WM = WM;
})();
