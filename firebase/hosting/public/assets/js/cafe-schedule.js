// 카페 상세(/cafe)의 '스케줄' 탭. 예전 /schedule 페이지의 조회 기능(주간·월간, 캐스트 필터, 이미지·엑셀 저장)을 옮겨 왔다.
// 로그인 없이 볼 수 있다 (castSchedules·guestCastSchedules는 공개 읽기).
// admin-common.js 다음, 페이지 스크립트 앞에 로드한다.
(function () {
  const A = window.ConcafeAdmin;
  const escapeHtml = A.escapeHtml;
  const normalizeText = A.normalizeText;
  const WEEKDAYS = ["월", "화", "수", "목", "금", "토", "일"];
  const WEEK_ORDINALS = ["첫째", "둘째", "셋째", "넷째", "다섯째", "여섯째"];
  const HTML2CANVAS_URL = "https://cdn.jsdelivr.net/npm/html2canvas@1.4.1/dist/html2canvas.min.js";

  function pad2(value) {
    return String(value).padStart(2, "0");
  }

  function dateKey(date) {
    return date.getFullYear() + "-" + pad2(date.getMonth() + 1) + "-" + pad2(date.getDate());
  }

  function startOfWeek(date) {
    const start = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
    return start;
  }

  function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
  }

  function weekOrdinal(date) {
    const firstDay = new Date(date.getFullYear(), date.getMonth(), 1).getDay();
    const index = Math.floor((((firstDay + 6) % 7) + date.getDate() - 1) / 7);
    return WEEK_ORDINALS[Math.min(WEEK_ORDINALS.length - 1, index)];
  }

  function timeText(item) {
    return item.startTime && item.endTime ? item.startTime + " - " + item.endTime : item.startTime || "";
  }

  let html2canvasPromise = null;
  function loadHtml2canvas() {
    if (window.html2canvas) {
      return Promise.resolve(window.html2canvas);
    }
    if (!html2canvasPromise) {
      html2canvasPromise = new Promise(function (resolve, reject) {
        const script = document.createElement("script");
        script.src = HTML2CANVAS_URL;
        script.onload = function () { resolve(window.html2canvas); };
        script.onerror = function () {
          html2canvasPromise = null;
          reject(new Error("html2canvas load failed"));
        };
        document.head.appendChild(script);
      });
    }
    return html2canvasPromise;
  }

  /**
   * @param {object} options
   * @param {HTMLElement} options.mount 스케줄 UI를 그릴 요소
   * @param {string} options.cafeId
   * @param {function} [options.onCount] 표시 기간의 근무 인원 수가 바뀔 때 호출된다
   * @param {string} [options.castId] 한 캐스트의 스케줄만 보여줄 때 (캐스트 상세). 캐스트 선택을 숨기고 게스트는 빼고 보여준다
   * @param {HTMLElement} [options.summaryMount] 오늘 출근 캐스트와 이번 주 출근 인원을 요약해 그릴 요소 (카페 상세)
   * @param {function} [options.onSelectDay] 요약의 날짜를 눌렀을 때 호출된다 (Date)
   * @param {function} [options.castHref] 요약의 캐스트를 누르면 갈 주소 (castId → URL)
   */
  function create(options) {
    const opts = options || {};
    const state = {
      cafeName: "",
      view: "week",
      anchor: new Date(),
      castId: opts.castId || "",
      casts: [],
      castById: {},
      schedules: [],
      guestSchedules: [],
      loaded: false,
      failed: false,
    };

    opts.mount.innerHTML =
      '<div class="sched-toolbar">' +
      '<div class="sched-nav">' +
      '<button class="button button-ghost sched-nav-button" type="button" data-sched-move="-1" aria-label="이전">‹</button>' +
      '<strong class="sched-period" data-role="period" aria-live="polite"></strong>' +
      '<button class="button button-ghost sched-nav-button" type="button" data-sched-move="1" aria-label="다음">›</button>' +
      '<button class="button button-ghost sched-today" type="button" data-sched-today>오늘</button>' +
      "</div>" +
      '<div class="sched-options">' +
      '<div class="filter-chips" role="radiogroup" aria-label="보기">' +
      '<button class="filter-chip is-active" type="button" role="radio" aria-checked="true" data-sched-view="week">주간</button>' +
      '<button class="filter-chip" type="button" role="radio" aria-checked="false" data-sched-view="month">월간</button>' +
      "</div>" +
      '<select data-role="cast" aria-label="캐스트 필터"><option value="">전체 캐스트</option></select>' +
      "</div>" +
      "</div>" +
      '<div class="sched-capture" data-role="capture">' +
      '<div class="sched-capture-head">' +
      '<h3 class="sched-title" data-role="title"></h3>' +
      '<div class="sched-legend"><span class="sched-chip">캐스트</span><span class="sched-chip guest">게스트</span></div>' +
      "</div>" +
      '<div data-role="body"></div>' +
      "</div>" +
      '<div class="sched-footer">' +
      '<button class="button button-secondary" type="button" data-sched-download="image">이미지로 저장</button>' +
      '<button class="button button-secondary" type="button" data-sched-download="excel">엑셀로 저장</button>' +
      "</div>";

    const el = {};
    ["period", "cast", "capture", "title", "body"].forEach(function (role) {
      el[role] = opts.mount.querySelector('[data-role="' + role + '"]');
    });

    // 날짜(YYYY-MM-DD)별 근무 목록. 캐스트 필터를 고르면 게스트는 빼고, 같은 날 같은 이름은 한 번만 넣는다
    function entriesByDate(filterCastId) {
      const castFilter = filterCastId === undefined ? state.castId : filterCastId;
      const map = {};
      const seen = {};
      function add(date, entry) {
        const key = date + "|" + entry.name;
        if (seen[key]) return;
        seen[key] = true;
        (map[date] = map[date] || []).push(entry);
      }
      state.schedules.forEach(function (schedule) {
        if (!schedule.date || schedule.status !== "WORK") return;
        if (castFilter && schedule.castId !== castFilter) return;
        if (!state.castNameById[schedule.castId]) return;
        const cast = state.castById[schedule.castId];
        add(schedule.date, {
          name: state.castNameById[schedule.castId],
          time: timeText(schedule),
          guest: false,
          castId: schedule.castId,
          photo: cast ? cast.profileImage : "",
        });
      });
      if (!castFilter) {
        state.guestSchedules.forEach(function (schedule) {
          if (!schedule.date) return;
          add(schedule.date, { name: schedule.name, time: timeText(schedule), guest: true, castId: "", photo: schedule.profileImage });
        });
      }
      Object.keys(map).forEach(function (date) {
        map[date].sort(function (a, b) {
          return (a.guest - b.guest) || a.name.localeCompare(b.name, "ko");
        });
      });
      return map;
    }

    // 현재 보기의 날짜 목록. 월간은 주 단위로 맞춘 앞뒤 빈칸을 null로 채운다
    function visibleDays() {
      if (state.view === "week") {
        const start = startOfWeek(state.anchor);
        return WEEKDAYS.map(function (_, index) { return addDays(start, index); });
      }
      const year = state.anchor.getFullYear();
      const month = state.anchor.getMonth();
      const days = [];
      const offset = (new Date(year, month, 1).getDay() + 6) % 7;
      for (let i = 0; i < offset; i += 1) days.push(null);
      const count = new Date(year, month + 1, 0).getDate();
      for (let day = 1; day <= count; day += 1) days.push(new Date(year, month, day));
      while (days.length % 7) days.push(null);
      return days;
    }

    function periodLabel() {
      if (state.view === "month") {
        return state.anchor.getFullYear() + "년 " + (state.anchor.getMonth() + 1) + "월";
      }
      const start = startOfWeek(state.anchor);
      const end = addDays(start, 6);
      // 주의 기준 월은 목요일이 속한 달로 잡는다
      const middle = addDays(start, 3);
      return (middle.getMonth() + 1) + "월 " + weekOrdinal(middle) + " 주 · " +
        (start.getMonth() + 1) + "/" + start.getDate() + " - " + (end.getMonth() + 1) + "/" + end.getDate();
    }

    function chipsHtml(entries, compact) {
      return entries.map(function (entry) {
        return '<span class="sched-chip' + (entry.guest ? " guest" : "") + '"' +
          (entry.time ? ' title="' + escapeHtml(entry.name + " " + entry.time) + '"' : "") + ">" +
          escapeHtml(entry.name) +
          (!compact && entry.time ? "<small>" + escapeHtml(entry.time) + "</small>" : "") +
          "</span>";
      }).join("");
    }

    function render() {
      el.period.textContent = periodLabel();
      Array.prototype.forEach.call(opts.mount.querySelectorAll("[data-sched-view]"), function (chip) {
        const active = chip.getAttribute("data-sched-view") === state.view;
        chip.classList.toggle("is-active", active);
        chip.setAttribute("aria-checked", active ? "true" : "false");
      });
      const castName = state.castId ? state.castNameById[state.castId] : "";
      el.title.textContent = [state.cafeName, castName, periodLabel().split(" · ")[0] + " 스케줄"].filter(Boolean).join(" · ");

      if (!state.loaded) {
        el.body.innerHTML = '<p class="empty">' + (state.failed ? "스케줄을 불러오지 못했습니다." : "스케줄을 불러오는 중입니다.") + "</p>";
        report(0);
        return;
      }

      const map = entriesByDate();
      const todayKey = dateKey(new Date());
      const days = visibleDays();
      let count = 0;
      const cells = days.map(function (date, index) {
        if (!date) {
          return '<div class="sched-cell is-outside" aria-hidden="true"></div>';
        }
        const key = dateKey(date);
        const entries = map[key] || [];
        count += entries.length;
        const weekday = index % 7;
        const classes = "sched-cell" + (key === todayKey ? " is-today" : "") +
          (weekday === 5 ? " is-sat" : weekday === 6 ? " is-sun" : "") + (entries.length ? "" : " is-empty");
        const head = state.view === "week"
          ? '<div class="sched-cell-head"><span class="sched-weekday">' + WEEKDAYS[weekday] + "</span>" +
            '<span class="sched-date">' + (date.getMonth() + 1) + "/" + date.getDate() + "</span></div>"
          : '<div class="sched-cell-head"><span class="sched-date">' + date.getDate() + "</span>" +
            (entries.length ? '<span class="sched-count">' + entries.length + "명</span>" : "") + "</div>";
        return '<div class="' + classes + '">' + head +
          '<div class="sched-chips">' + (entries.length ? chipsHtml(entries, state.view === "month") : '<span class="sched-none">-</span>') + "</div>" +
          "</div>";
      }).join("");

      el.body.innerHTML = state.view === "week"
        ? '<div class="sched-week">' + cells + "</div>"
        : '<div class="sched-month"><div class="sched-month-head">' +
          WEEKDAYS.map(function (label) { return "<span>" + label + "</span>"; }).join("") +
          "</div>" + '<div class="sched-month-grid">' + cells + "</div></div>";
      if (!count) {
        el.body.insertAdjacentHTML("beforeend", '<p class="helper sched-empty-note">이 기간에 등록된 근무 스케줄이 없습니다.</p>');
      }
      report(count);
      renderSummary();
    }

    // ---------- 요약 (카페 상세 상단의 '스케줄 현황') ----------

    function avatarHtml(entry) {
      return '<span class="sched-avatar">' + (entry.photo
        ? '<img src="' + escapeHtml(entry.photo) + '" alt="" loading="lazy" />'
        : escapeHtml(entry.name.slice(0, 1))) + "</span>";
    }

    function renderSummary() {
      if (!opts.summaryMount) {
        return;
      }
      if (!state.loaded) {
        opts.summaryMount.innerHTML = '<p class="empty">' + (state.failed ? "스케줄을 불러오지 못했습니다." : "스케줄을 불러오는 중입니다.") + "</p>";
        return;
      }
      const map = entriesByDate(opts.castId || "");
      const today = new Date();
      const todayKey = dateKey(today);
      const todayEntries = map[todayKey] || [];
      const weekStart = startOfWeek(today);
      const todayLabel = (today.getMonth() + 1) + "/" + today.getDate() + " (" + WEEKDAYS[(today.getDay() + 6) % 7] + ")";

      const people = todayEntries.length
        ? todayEntries.map(function (entry) {
            const inner = avatarHtml(entry) +
              '<span class="sched-person-text"><strong>' + escapeHtml(entry.name) + "</strong>" +
              "<small>" + escapeHtml(entry.guest ? "게스트" + (entry.time ? " · " + entry.time : "") : entry.time || "시간 미정") + "</small></span>";
            return entry.castId && opts.castHref
              ? '<a class="sched-person" href="' + escapeHtml(opts.castHref(entry.castId)) + '">' + inner + "</a>"
              : '<div class="sched-person' + (entry.guest ? " guest" : "") + '">' + inner + "</div>";
          }).join("")
        : '<p class="helper sched-sum-none">오늘 등록된 출근 캐스트가 없습니다.</p>';

      const strip = WEEKDAYS.map(function (label, index) {
        const date = addDays(weekStart, index);
        const key = dateKey(date);
        const count = (map[key] || []).length;
        const classes = "sched-sum-day" + (key === todayKey ? " is-today" : "") + (key < todayKey ? " is-past" : "") +
          (index === 5 ? " is-sat" : index === 6 ? " is-sun" : "");
        return '<button class="' + classes + '" type="button" data-sched-day="' + key + '">' +
          '<span class="sched-sum-weekday">' + label + "</span>" +
          '<span class="sched-sum-date">' + (date.getMonth() + 1) + "/" + date.getDate() + "</span>" +
          '<strong class="sched-sum-count">' + (count ? count + "명" : "-") + "</strong></button>";
      }).join("");

      opts.summaryMount.innerHTML =
        '<div class="sched-summary">' +
        '<div class="sched-sum-today">' +
        '<div class="sched-sum-label">오늘 출근 <span>' + escapeHtml(todayLabel) + " · " + todayEntries.length + "명</span></div>" +
        '<div class="sched-people">' + people + "</div>" +
        "</div>" +
        '<div class="sched-sum-week">' +
        '<div class="sched-sum-label">이번 주 출근 인원</div>' +
        '<div class="sched-sum-strip">' + strip + "</div>" +
        "</div>" +
        "</div>";
    }

    if (opts.summaryMount) {
      opts.summaryMount.addEventListener("click", function (e) {
        const day = e.target.closest("[data-sched-day]");
        if (day && opts.onSelectDay) {
          opts.onSelectDay(new Date(day.getAttribute("data-sched-day") + "T00:00:00"));
        }
      });
    }

    function report(count) {
      if (opts.onCount) {
        opts.onCount(count);
      }
    }

    function renderCastOptions() {
      el.cast.innerHTML = '<option value="">전체 캐스트</option>' + state.casts.map(function (cast) {
        return '<option value="' + escapeHtml(cast.id) + '">' + escapeHtml(cast.name) + "</option>";
      }).join("");
      el.cast.value = state.castId;
    }

    // ---------- 저장 ----------

    function fileName(ext) {
      const now = new Date();
      const stamp = String(now.getFullYear()) + pad2(now.getMonth() + 1) + pad2(now.getDate());
      const period = state.view === "month"
        ? state.anchor.getFullYear() + pad2(state.anchor.getMonth() + 1)
        : dateKey(startOfWeek(state.anchor)).replace(/-/g, "") + "_주간";
      const castPart = opts.castId && state.castNameById[opts.castId] ? "_" + state.castNameById[opts.castId] : "";
      return (state.cafeName + castPart + "_" + period + "_스케줄_" + stamp + "." + ext).replace(/[\\/:*?"<>|]/g, "_");
    }

    function triggerDownload(href, name) {
      const link = document.createElement("a");
      link.href = href;
      link.download = name;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }

    function downloadExcel() {
      const map = entriesByDate();
      const days = visibleDays();
      const rows = [WEEKDAYS.slice()];
      for (let i = 0; i < days.length; i += 7) {
        rows.push(days.slice(i, i + 7).map(function (date) {
          if (!date) return "";
          const names = (map[dateKey(date)] || []).map(function (entry) {
            return entry.name + (entry.guest ? " (게스트)" : "") + (entry.time ? " " + entry.time : "");
          });
          return (date.getMonth() + 1) + "/" + date.getDate() + "\n" + (names.length ? names.join("\n") : "-");
        }));
      }
      const cell = function (value) { return "<td>" + escapeHtml(value).replace(/\n/g, "<br>") + "</td>"; };
      const html = "﻿" + '<html><head><meta charset="UTF-8" /></head><body><table border="1">' +
        '<tr><th colspan="7">' + escapeHtml(el.title.textContent) + "</th></tr>" +
        rows.map(function (row) { return "<tr>" + row.map(cell).join("") + "</tr>"; }).join("") +
        "</table></body></html>";
      const url = URL.createObjectURL(new Blob([html], { type: "application/vnd.ms-excel;charset=utf-8" }));
      triggerDownload(url, fileName("xls"));
      URL.revokeObjectURL(url);
      window.ConcafeToast.show("success", "스케줄 엑셀 파일을 저장했습니다.");
    }

    async function downloadImage() {
      try {
        const html2canvas = await loadHtml2canvas();
        const canvas = await html2canvas(el.capture, { scale: 2, backgroundColor: "#ffffff", useCORS: true });
        triggerDownload(canvas.toDataURL("image/png"), fileName("png"));
        window.ConcafeToast.show("success", "스케줄 이미지를 저장했습니다.");
      } catch (error) {
        console.error(error);
        window.ConcafeToast.show("error", "스케줄 이미지를 만들지 못했습니다.");
      }
    }

    // ---------- 이벤트 연결 ----------

    opts.mount.addEventListener("click", function (e) {
      const move = e.target.closest("[data-sched-move]");
      if (move) {
        const step = Number(move.getAttribute("data-sched-move"));
        state.anchor = state.view === "week"
          ? addDays(state.anchor, step * 7)
          : new Date(state.anchor.getFullYear(), state.anchor.getMonth() + step, 1);
        render();
        return;
      }
      if (e.target.closest("[data-sched-today]")) {
        state.anchor = new Date();
        render();
        return;
      }
      const view = e.target.closest("[data-sched-view]");
      if (view) {
        state.view = view.getAttribute("data-sched-view");
        render();
        return;
      }
      const download = e.target.closest("[data-sched-download]");
      if (download) {
        if (!state.loaded) return;
        if (download.getAttribute("data-sched-download") === "image") {
          downloadImage();
        } else {
          downloadExcel();
        }
      }
    });
    el.cast.addEventListener("change", function () {
      state.castId = el.cast.value;
      render();
    });

    state.castNameById = {};
    if (opts.castId) {
      // 캐스트 상세에서는 캐스트 선택이 필요 없다
      el.cast.style.display = "none";
      opts.mount.querySelector(".sched-legend").style.display = "none";
    }
    render();

    return {
      // 카페와 캐스트 전체(toCast 결과, 현재 없는 캐스트 포함)를 넣고 스케줄을 불러온다.
      // 지난 스케줄 이름 표시에는 전체를, 캐스트 필터에는 활동 중인 캐스트만 쓴다
      load: async function (cafe, casts) {
        state.cafeName = cafe.name;
        state.casts = casts.filter(function (cast) { return !cast.inactive; });
        state.castNameById = {};
        state.castById = {};
        casts.forEach(function (cast) {
          state.castNameById[cast.id] = cast.name;
          state.castById[cast.id] = cast;
        });
        renderCastOptions();
        render();
        try {
          const results = await Promise.allSettled([
            A.db.collection("castSchedules").where("cafeId", "==", opts.cafeId).get(),
            A.db.collection("guestCastSchedules").where("cafeId", "==", opts.cafeId).get(),
          ]);
          if (results[0].status === "rejected") {
            throw results[0].reason;
          }
          state.schedules = results[0].value.docs.map(function (doc) {
            const data = doc.data() || {};
            return {
              castId: normalizeText(data.castId, ""),
              date: normalizeText(data.date, ""),
              status: normalizeText(data.status, "OFF"),
              startTime: normalizeText(data.startTime, ""),
              endTime: normalizeText(data.endTime, ""),
            };
          });
          if (results[1].status === "rejected") {
            console.error("Failed to load guest cast schedules", results[1].reason);
          }
          state.guestSchedules = results[1].status === "fulfilled" ? results[1].value.docs.map(function (doc) {
            const data = doc.data() || {};
            return {
              date: normalizeText(data.date, ""),
              name: normalizeText(data.name, "게스트"),
              profileImage: normalizeText(data.profileImage, ""),
              startTime: normalizeText(data.startTime, ""),
              endTime: normalizeText(data.endTime, ""),
            };
          }) : [];
          state.loaded = true;
        } catch (error) {
          console.error(error);
          state.failed = true;
        }
        render();
      },
      fail: function () {
        state.failed = true;
        render();
      },
      // 주간 보기로 바꾸고 date가 있는 주를 보여준다 (요약의 날짜를 눌렀을 때)
      focusDate: function (date) {
        state.view = "week";
        state.anchor = date;
        render();
      },
    };
  }

  window.ConcafeSchedule = { create: create };
})();
