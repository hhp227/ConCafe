// 카페 목록(공개 /cafes, 관리자 /admin)이 함께 쓰는 필터 컴포넌트와 카페 카드 렌더링.
// 국가·도시 칩, 이름·주소 검색, 컨셉 선택을 mount 요소 안에 그리고, 선택 상태를 sessionStorage에 유지한다.
// admin-common.js 다음, 각 페이지 스크립트 앞에 로드한다.
(function () {
  const A = window.ConcafeAdmin;
  const escapeHtml = A.escapeHtml;
  const COUNTRY_ORDER = ["KR", "JP"];

  function readStorage(key) {
    if (!key) return null;
    try {
      return sessionStorage.getItem(key);
    } catch (error) {
      return null;
    }
  }

  function writeStorage(key, value) {
    if (!key) return;
    try {
      sessionStorage.setItem(key, value);
    } catch (error) {
      // 저장소를 쓸 수 없으면 이번 화면에서만 유지한다
    }
  }

  // 접속자의 나라를 브라우저 시간대(우선)와 언어 설정으로 추정한다. 위치 권한은 묻지 않는다
  function detectVisitorCountry() {
    let timeZone = "";
    try {
      timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || "";
    } catch (error) {
      timeZone = "";
    }
    if (timeZone === "Asia/Seoul") return "KR";
    if (timeZone === "Asia/Tokyo") return "JP";
    const languages = navigator.languages && navigator.languages.length ? navigator.languages : [navigator.language || ""];
    for (const language of languages) {
      const code = String(language || "").toLowerCase();
      if (code.indexOf("ko") === 0) return "KR";
      if (code.indexOf("ja") === 0) return "JP";
    }
    return "";
  }

  function countryLabel(country) {
    return A.COUNTRY_LABELS[country] || country;
  }

  function chipHtml(attr, value, label, count, active, disabled) {
    return '<button class="filter-chip' + (active ? " is-active" : "") + (count === 0 ? " is-empty" : "") +
      '" type="button" role="radio" aria-checked="' + active + '" ' + attr + '="' + escapeHtml(value) + '"' +
      (disabled ? " disabled" : "") + ">" +
      escapeHtml(label) + ' <span class="chip-count">' + count + "</span></button>";
  }

  /**
   * @param {object} options
   * @param {HTMLElement} options.mount 필터 UI를 그릴 요소
   * @param {string} [options.searchPlaceholder]
   * @param {string} [options.searchStorageKey] 검색어를 유지할 sessionStorage 키
   * @param {string} [options.regionStorageKey] 국가·도시 선택을 유지할 sessionStorage 키
   * @param {boolean} [options.detectCountry] 저장된 선택이 없을 때 접속 지역 나라로 시작할지
   * @param {boolean} [options.disabled] 처음에 비활성 상태로 그릴지
   * @param {function} options.onChange 필터가 바뀔 때 호출된다
   */
  function createFilter(options) {
    const opts = options || {};
    const onChange = opts.onChange || function () {};
    const state = {
      cafes: [],
      country: "",
      city: "",
      detectedCountry: "",
      regionChosen: false,
      enabled: !opts.disabled,
    };

    opts.mount.innerHTML =
      '<div class="region-filter">' +
      '<div class="filter-chips" data-role="country" role="radiogroup" aria-label="국가"></div>' +
      '<div class="filter-chips sub" data-role="city" role="radiogroup" aria-label="도시" hidden></div>' +
      '<p class="helper small" data-role="note"></p>' +
      "</div>" +
      '<div class="cafe-toolbar">' +
      '<input data-role="search" type="search" aria-label="카페 검색" />' +
      '<select data-role="concept" aria-label="컨셉 필터"><option value="">모든 컨셉</option>' +
      A.CONCEPT_TYPES.map(function (type) {
        return '<option value="' + type[0] + '">' + escapeHtml(type[1]) + "</option>";
      }).join("") +
      "</select></div>";

    const el = {};
    ["country", "city", "note", "search", "concept"].forEach(function (role) {
      el[role] = opts.mount.querySelector('[data-role="' + role + '"]');
    });
    el.search.placeholder = opts.searchPlaceholder || "카페 이름 또는 주소로 검색";
    el.search.value = readStorage(opts.searchStorageKey) || "";

    function saveRegion() {
      writeStorage(opts.regionStorageKey, JSON.stringify({ country: state.country, city: state.city }));
    }

    function loadSavedRegion() {
      try {
        const saved = JSON.parse(readStorage(opts.regionStorageKey) || "null");
        if (saved && typeof saved.country === "string" && typeof saved.city === "string") {
          return saved;
        }
      } catch (error) {
        // 읽을 수 없으면 저장값 없이 시작한다
      }
      return null;
    }

    function countriesInData() {
      const list = COUNTRY_ORDER.slice();
      state.cafes.forEach(function (cafe) {
        if (cafe.country && list.indexOf(cafe.country) < 0) {
          list.push(cafe.country);
        }
      });
      return list;
    }

    // 나라별 도시 목록: 미리 정한 도시 + 데이터에만 있는 도시, '기타'는 맨 뒤
    function citiesOf(country) {
      const known = (A.CITIES[country] || []).map(function (city) { return city[0]; });
      const extra = [];
      state.cafes.forEach(function (cafe) {
        if (cafe.country === country && cafe.city && known.indexOf(cafe.city) < 0 && extra.indexOf(cafe.city) < 0) {
          extra.push(cafe.city);
        }
      });
      const ordered = known.filter(function (city) { return city !== "Etc"; }).concat(extra.sort());
      return known.indexOf("Etc") >= 0 ? ordered.concat(["Etc"]) : ordered;
    }

    function renderRegion() {
      const disabled = !state.enabled;
      const countByCountry = {};
      const countByCity = {};
      state.cafes.forEach(function (cafe) {
        countByCountry[cafe.country] = (countByCountry[cafe.country] || 0) + 1;
        const cityKey = cafe.country + ":" + cafe.city;
        countByCity[cityKey] = (countByCity[cityKey] || 0) + 1;
      });
      el.country.innerHTML = chipHtml("data-country", "", "전체", state.cafes.length, !state.country, disabled) +
        countriesInData().map(function (country) {
          return chipHtml("data-country", country, countryLabel(country), countByCountry[country] || 0, state.country === country, disabled);
        }).join("");

      el.city.hidden = !state.country;
      el.city.innerHTML = state.country
        ? chipHtml("data-city", "", countryLabel(state.country) + " 전체", countByCountry[state.country] || 0, !state.city, disabled) +
          citiesOf(state.country).map(function (city) {
            return chipHtml("data-city", city, A.cityLabel(state.country, city), countByCity[state.country + ":" + city] || 0, state.city === city, disabled);
          }).join("")
        : "";

      el.note.textContent = !state.regionChosen && state.detectedCountry && state.country === state.detectedCountry
        ? "접속한 지역에 맞춰 " + countryLabel(state.country) + " 카페를 먼저 보여줍니다. 다른 나라는 위에서 고를 수 있습니다."
        : "";
    }

    // 저장된 선택 → (detectCountry면) 접속 지역 → 전체 순으로 시작 필터를 정한다. 카페가 없는 나라는 건너뛴다
    function initRegion() {
      state.detectedCountry = opts.detectCountry ? detectVisitorCountry() : "";
      const saved = loadSavedRegion();
      const hasCafes = function (country) {
        return state.cafes.some(function (cafe) { return cafe.country === country; });
      };
      state.regionChosen = false;
      if (saved && (!saved.country || hasCafes(saved.country))) {
        state.country = saved.country;
        state.city = saved.country ? saved.city : "";
        state.regionChosen = true;
      } else if (state.detectedCountry && hasCafes(state.detectedCountry)) {
        state.country = state.detectedCountry;
        state.city = "";
      } else {
        state.country = "";
        state.city = "";
      }
    }

    function setRegion(country, city) {
      state.country = country;
      state.city = country ? city : "";
      state.regionChosen = true;
      saveRegion();
      renderRegion();
      onChange();
    }

    el.country.addEventListener("click", function (e) {
      const chip = e.target.closest("[data-country]");
      if (chip && !chip.disabled) {
        setRegion(chip.getAttribute("data-country"), "");
      }
    });
    el.city.addEventListener("click", function (e) {
      const chip = e.target.closest("[data-city]");
      if (chip && !chip.disabled) {
        setRegion(state.country, chip.getAttribute("data-city"));
      }
    });
    el.search.addEventListener("input", function () {
      writeStorage(opts.searchStorageKey, el.search.value);
      onChange();
    });
    el.concept.addEventListener("change", onChange);

    function setEnabled(enabled) {
      state.enabled = enabled;
      el.search.disabled = !enabled;
      el.concept.disabled = !enabled;
      renderRegion();
    }

    setEnabled(state.enabled);

    return {
      // 카페 목록을 넣으면 시작 지역을 정하고 칩 개수를 다시 그린다
      setCafes: function (cafes) {
        state.cafes = cafes || [];
        initRegion();
        renderRegion();
      },
      setEnabled: setEnabled,
      apply: function (cafes) {
        const list = cafes || state.cafes;
        const keyword = el.search.value.trim().toLowerCase();
        const concept = el.concept.value;
        return list.filter(function (cafe) {
          if (state.country && cafe.country !== state.country) return false;
          if (state.city && cafe.city !== state.city) return false;
          if (concept && cafe.conceptType !== concept) return false;
          return !keyword || (cafe.name + " " + cafe.regionLabel).toLowerCase().indexOf(keyword) >= 0;
        });
      },
      // 검색 결과가 없을 때 보여줄 문구
      emptyMessage: function () {
        return state.city
          ? A.cityLabel(state.country, state.city) + "에 조건에 맞는 카페가 없습니다."
          : "검색 결과가 없습니다.";
      },
    };
  }

  /**
   * 카페 카드 한 장의 HTML.
   * @param {object} cafe toCafe 결과
   * @param {object} options
   * @param {string} options.href
   * @param {string} [options.extraChipsHtml] 컨셉 칩 뒤에 붙일 칩 HTML
   */
  function cardHtml(cafe, options) {
    const opts = options || {};
    const thumb = cafe.thumbnailImage
      ? '<img src="' + escapeHtml(cafe.thumbnailImage) + '" alt="" loading="lazy" />'
      : escapeHtml(cafe.name.slice(0, 1));
    const concept = A.conceptLabel(cafe.conceptType);
    const region = cafe.address || A.cityLabel(cafe.country, cafe.city) || "지역 정보 없음";
    return '<a class="cafe-card" href="' + escapeHtml(opts.href) + '" data-cafe-id="' + escapeHtml(cafe.id) + '">' +
      '<div class="cafe-card-thumb">' + thumb + "</div>" +
      '<div class="cafe-card-body">' +
      '<h3 class="cafe-card-name" title="' + escapeHtml(cafe.name) + '">' + escapeHtml(cafe.name) + "</h3>" +
      '<p class="cafe-card-region">' + escapeHtml(region) + "</p>" +
      '<div class="chip-row">' +
      (concept ? '<span class="chip">' + escapeHtml(concept) + "</span>" : "") +
      (opts.extraChipsHtml || "") +
      "</div></div></a>";
  }

  window.ConcafeCafeList = {
    createFilter: createFilter,
    cardHtml: cardHtml,
  };
})();
