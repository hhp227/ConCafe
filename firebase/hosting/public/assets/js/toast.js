// 화면 아래쪽에 잠시 나타났다 사라지는 토스트(스낵바) 알림.
// ConcafeToast.show(type, message, { key, duration, action: { label, onClick } })
//   type: "success" | "info" | "error" | "loading"
//   - 같은 key로 다시 부르면 새로 쌓지 않고 기존 알림 내용을 바꾼다.
//   - loading은 작업이 끝날 때까지 남아 있고, 다른 알림(성공·실패 등)이 뜨면 자동으로 닫힌다.
// ConcafeToast.dismiss(key)
// ConcafeToast.flash(type, message) — 다음에 열리는 페이지에서 보여줄 알림 (이동 직전에 사용)
(function () {
  if (window.ConcafeToast) {
    return;
  }

  const DURATIONS = { success: 3000, info: 3500, error: 6000 };
  const LOADING_SAFETY_MS = 120000;
  const MAX_VISIBLE = 4;
  const ICONS = {
    success: '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z"/></svg>',
    error: '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M11 15h2v2h-2zm0-8h2v6h-2zm1-5C6.47 2 2 6.5 2 12a10 10 0 1 0 10-10zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16z"/></svg>',
    info: '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M11 7h2v2h-2zm0 4h2v6h-2zm1-9a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16z"/></svg>',
    loading: '<span class="cc-toast-spinner" aria-hidden="true"></span>',
  };

  const STYLE = [
    ".cc-toast-region{position:fixed;left:50%;bottom:24px;transform:translateX(-50%);z-index:10000;display:flex;flex-direction:column-reverse;align-items:stretch;gap:10px;width:max-content;max-width:calc(100vw - 32px);pointer-events:none;}",
    ".cc-toast{pointer-events:auto;display:flex;align-items:center;gap:10px;min-width:260px;max-width:520px;padding:12px 14px 12px 16px;border-radius:14px;background:#2b1d24;color:#fff;font:inherit;font-size:14px;line-height:1.45;box-shadow:0 12px 32px rgba(35,22,28,.28);opacity:0;transform:translateY(12px);transition:opacity .2s ease,transform .2s ease;}",
    ".cc-toast.is-visible{opacity:1;transform:translateY(0);}",
    ".cc-toast-icon{flex:0 0 20px;width:20px;height:20px;display:flex;align-items:center;justify-content:center;}",
    ".cc-toast-icon svg{width:20px;height:20px;}",
    ".cc-toast.success .cc-toast-icon{color:#6ee7b0;}",
    ".cc-toast.error{background:#4a1626;}",
    ".cc-toast.error .cc-toast-icon{color:#ff9db4;}",
    ".cc-toast.info .cc-toast-icon{color:#ffc4d8;}",
    ".cc-toast-message{flex:1 1 auto;word-break:keep-all;overflow-wrap:anywhere;}",
    ".cc-toast-action{flex:0 0 auto;border:0;background:transparent;color:#ff9ec0;font:inherit;font-size:13px;font-weight:800;cursor:pointer;padding:6px 8px;border-radius:8px;}",
    ".cc-toast-action:hover{background:rgba(255,255,255,.08);}",
    ".cc-toast-close{flex:0 0 auto;border:0;background:transparent;color:rgba(255,255,255,.6);font-size:14px;line-height:1;cursor:pointer;padding:6px;border-radius:8px;}",
    ".cc-toast-close:hover{color:#fff;background:rgba(255,255,255,.08);}",
    ".cc-toast-spinner{width:16px;height:16px;border:2px solid rgba(255,255,255,.28);border-top-color:#ff9ec0;border-radius:50%;animation:cc-toast-spin .8s linear infinite;}",
    "@keyframes cc-toast-spin{to{transform:rotate(360deg);}}",
    "@media (max-width:560px){.cc-toast-region{bottom:16px;width:calc(100vw - 32px);}.cc-toast{min-width:0;width:100%;}}",
    "@media (prefers-reduced-motion:reduce){.cc-toast{transition:none;}.cc-toast-spinner{animation-duration:2s;}}",
  ].join("\n");

  const toasts = new Map();
  let region = null;
  let seq = 0;

  function ensureRegion() {
    if (region) {
      return region;
    }
    const style = document.createElement("style");
    style.textContent = STYLE;
    document.head.appendChild(style);
    region = document.createElement("div");
    region.className = "cc-toast-region";
    region.setAttribute("aria-live", "polite");
    document.body.appendChild(region);
    return region;
  }

  function clearTimer(entry) {
    if (entry.timer) {
      clearTimeout(entry.timer);
      entry.timer = null;
    }
  }

  function startTimer(entry) {
    clearTimer(entry);
    const duration = entry.type === "loading" ? LOADING_SAFETY_MS : entry.duration;
    entry.remaining = duration;
    entry.startedAt = Date.now();
    entry.timerType = entry.type;
    entry.timer = setTimeout(function () { dismiss(entry.key); }, duration);
  }

  function dismiss(key) {
    const entry = toasts.get(key);
    if (!entry) {
      return;
    }
    toasts.delete(key);
    clearTimer(entry);
    entry.node.classList.remove("is-visible");
    setTimeout(function () { entry.node.remove(); }, 220);
  }

  function render(entry) {
    const node = entry.node;
    node.className = "cc-toast " + entry.type + " is-visible";
    node.setAttribute("role", entry.type === "error" ? "alert" : "status");
    node.innerHTML = "";
    const icon = document.createElement("span");
    icon.className = "cc-toast-icon";
    icon.innerHTML = ICONS[entry.type] || ICONS.info;
    const text = document.createElement("span");
    text.className = "cc-toast-message";
    text.textContent = entry.message;
    node.appendChild(icon);
    node.appendChild(text);
    if (entry.action && entry.action.label) {
      const action = document.createElement("button");
      action.type = "button";
      action.className = "cc-toast-action";
      action.textContent = entry.action.label;
      action.addEventListener("click", function () {
        dismiss(entry.key);
        entry.action.onClick();
      });
      node.appendChild(action);
    }
    if (entry.type !== "loading") {
      const close = document.createElement("button");
      close.type = "button";
      close.className = "cc-toast-close";
      close.setAttribute("aria-label", "알림 닫기");
      close.textContent = "✕";
      close.addEventListener("click", function () { dismiss(entry.key); });
      node.appendChild(close);
    }
  }

  function show(type, message, options) {
    const opts = options || {};
    const normalizedType = (DURATIONS[type] || type === "loading") ? type : "info";
    if (!message) {
      if (opts.key) {
        dismiss(opts.key);
      }
      return null;
    }
    ensureRegion();
    // 결과 알림이 뜨면 진행 중 알림은 할 일을 마친 것이므로 닫는다
    if (normalizedType !== "loading") {
      Array.from(toasts.values()).forEach(function (entry) {
        if (entry.type === "loading" && entry.key !== opts.key) {
          dismiss(entry.key);
        }
      });
    }
    const key = opts.key || "toast-" + (++seq);
    let entry = toasts.get(key);
    if (!entry) {
      const node = document.createElement("div");
      node.className = "cc-toast";
      entry = { key: key, node: node };
      node.addEventListener("mouseenter", function () {
        if (entry.type !== "loading" && entry.timer) {
          clearTimer(entry);
          entry.remaining = Math.max(1000, entry.remaining - (Date.now() - entry.startedAt));
        }
      });
      node.addEventListener("mouseleave", function () {
        if (entry.type !== "loading" && !entry.timer && toasts.has(entry.key)) {
          entry.startedAt = Date.now();
          entry.timer = setTimeout(function () { dismiss(entry.key); }, entry.remaining);
        }
      });
      toasts.set(key, entry);
      region.appendChild(node);
      // 너무 많이 쌓이면 오래된 알림부터 닫는다
      const keys = Array.from(toasts.keys());
      keys.slice(0, Math.max(0, keys.length - MAX_VISIBLE)).forEach(dismiss);
      requestAnimationFrame(function () { node.classList.add("is-visible"); });
    }
    const sameContent = entry.type === normalizedType && entry.message === message;
    entry.type = normalizedType;
    entry.message = message;
    entry.action = opts.action || null;
    // 긴 문장은 읽을 시간을 더 준다 (최대 10초)
    entry.duration = opts.duration || Math.max(DURATIONS[normalizedType] || DURATIONS.info, Math.min(10000, message.length * 70));
    if (!sameContent || !entry.rendered) {
      render(entry);
      entry.rendered = true;
    }
    // 진행 중 알림의 문구만 바뀔 때(경과 시간 등)는 타이머를 새로 시작하지 않는다
    if (!(normalizedType === "loading" && entry.timer && entry.timerType === "loading")) {
      startTimer(entry);
    }
    return key;
  }

  // 페이지를 옮기기 직전의 결과 알림을 다음 페이지에서 보여준다
  const FLASH_KEY = "concafe.flashToast";

  function flash(type, message) {
    try {
      sessionStorage.setItem(FLASH_KEY, JSON.stringify({ type: type, message: message, at: Date.now() }));
    } catch (error) {
      show(type, message);
    }
  }

  function showPendingFlash() {
    let pending = null;
    try {
      pending = JSON.parse(sessionStorage.getItem(FLASH_KEY) || "null");
      sessionStorage.removeItem(FLASH_KEY);
    } catch (error) {
      pending = null;
    }
    if (pending && pending.message && Date.now() - (pending.at || 0) < 30000) {
      show(pending.type, pending.message);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", showPendingFlash);
  } else {
    showPendingFlash();
  }

  window.ConcafeToast = { show: show, dismiss: dismiss, flash: flash };
})();
