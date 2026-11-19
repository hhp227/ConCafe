// 관리자 페이지(카페 목록 / 카페 등록 / 카페 상세)가 함께 쓰는 Firebase 초기화, 로그인, 권한 확인, 공통 유틸리티.
// firebase-*-compat 스크립트와 /config.js 다음, 각 페이지 스크립트와 admin-nav.js 앞에 로드한다.
(function () {
  const firebaseConfig = {
    apiKey: "AIzaSyCAsBqhoAHVR19CC9zTUp7SG9bpvczfzwI",
    authDomain: "concafe-5f7fd.firebaseapp.com",
    projectId: "concafe-5f7fd",
    storageBucket: "concafe-5f7fd.firebasestorage.app",
    messagingSenderId: "387905493709",
    appId: "1:387905493709:web:admin-page",
  };

  const FUNCTIONS_BASE_URL = "https://us-central1-concafe-5f7fd.cloudfunctions.net";
  const CONCEPT_TYPES = [
    ["MAID", "메이드"], ["BUTLER", "집사"], ["IDOL", "아이돌"], ["DEVIL", "악마"], ["DOLL", "인형"],
    ["COSPLAY", "코스프레"], ["NAMJANG", "남장"], ["YOKAI", "요괴"], ["CAT", "고양이"], ["OTHER", "기타"],
  ];
  const CITIES = {
    KR: [["Seoul", "서울"], ["Busan", "부산"], ["Daegu", "대구"], ["Etc", "기타"]],
    JP: [["Tokyo", "도쿄"], ["Osaka", "오사카"], ["Yokohama", "요코하마"], ["Etc", "기타"]],
  };
  const COUNTRY_LABELS = { KR: "한국", JP: "일본" };
  const CAST_ROSTER_STATUS_INACTIVE = "INACTIVE";
  // 앱(CastEdit)과 서버(scheduleImport)가 쓰는 캐스트 역할 값
  const CAST_CONCEPT_ROLES = [["maid", "메이드"], ["butler", "집사"], ["idol", "아이돌"]];
  // 앱(MenuGoodsEdit)과 같은 분류. goods만 goods 컬렉션, 나머지는 menus 컬렉션에 저장한다
  const MENU_CATEGORIES = [["drink", "음료"], ["food", "음식"], ["dessert", "디저트"], ["goods", "굿즈"]];
  const IMAGE_MAX_BYTES = 4.5 * 1024 * 1024;
  const IMAGE_MAX_EDGE = 3000;
  const IMAGE_SUPPORTED_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
  const ERROR_MESSAGES = {
    missing_auth: "로그인 정보가 없습니다. 다시 로그인하세요.",
    invalid_auth: "로그인이 만료되었습니다. 다시 로그인하세요.",
    forbidden: "이 카페를 관리할 권한이 없습니다.",
    cafe_not_found: "카페를 찾을 수 없습니다.",
    invalid_image_url: "업로드한 이미지 경로가 올바르지 않습니다.",
    image_not_found: "업로드한 이미지를 찾을 수 없습니다.",
    unsupported_image_type: "JPG, PNG, GIF, WEBP 이미지만 분석할 수 있습니다.",
    image_too_large: "이미지가 너무 큽니다 (최대 5MB).",
    not_schedule_image: "출근표 이미지로 인식되지 않았습니다.",
    not_event_image: "이벤트 안내 이미지로 인식되지 않았습니다. 출근표는 캐스트 스케줄 등록을 사용하세요.",
    no_schedule_days: "분석 가능한 날짜가 없습니다. 날짜가 보이는 출근표인지 확인하세요.",
    extraction_refused: "이미지 분석이 거절되었습니다. 다른 이미지로 시도하세요.",
    extraction_truncated: "출근표가 너무 길어 분석이 중간에 끊겼습니다. 이미지를 나눠서 올려주세요.",
    extraction_invalid: "분석 결과를 읽지 못했습니다. 다시 시도하세요.",
    extraction_busy: "분석 요청이 많습니다. 잠시 후 다시 시도하세요.",
    extraction_timeout: "분석 시간이 초과되었습니다. 다시 시도하세요.",
    extraction_failed: "이미지 분석에 실패했습니다. 잠시 후 다시 시도하세요.",
    import_not_found: "분석 결과가 만료되었거나 없습니다. 다시 분석하세요.",
    import_already_applied: "이미 저장한 분석 결과입니다. 새로 분석하세요.",
    entry_date_out_of_import: "출근표 기간 밖의 날짜가 포함되어 있습니다.",
    cast_not_found: "연결한 캐스트를 찾을 수 없습니다. 다시 분석하세요.",
    invalid_entries: "저장할 스케줄 형식이 올바르지 않습니다.",
    invalid_default_time: "기본 시작/종료 시간을 확인하세요.",
    invalid_inactive_casts: "현재 없는 캐스트 선택이 올바르지 않습니다.",
    inactive_cast_in_schedule: "출근표에 있는 캐스트는 현재 없는 캐스트로 분리할 수 없습니다.",
  };

  const app = firebase.initializeApp(firebaseConfig);
  const auth = app.auth();
  const db = app.firestore();
  const storage = app.storage();

  // ---------- 유틸리티 ----------

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  // 상태·결과 알림은 화면에 고정하지 않고 토스트로 잠깐 보여준다 (assets/js/toast.js).
  // key가 같은 알림은 새로 쌓지 않고 내용을 바꾼다. type: success | info | error | loading
  function notifyKey(target) {
    return typeof target === "string" ? target : (target && target.id) || "";
  }

  function setBanner(target, type, message, options) {
    const opts = options || {};
    window.ConcafeToast.show(type, message, { key: notifyKey(target) || undefined, action: opts.action, duration: opts.duration });
  }

  function hideBanner(target) {
    const key = notifyKey(target);
    if (key) {
      window.ConcafeToast.dismiss(key);
    }
  }

  function normalizeText(value, fallback) {
    return typeof value === "string" && value.trim() ? value.trim() : fallback;
  }

  function normalizeName(value) {
    return String(value || "").normalize("NFKC").toLowerCase().replace(/[\s\p{P}\p{S}]/gu, "");
  }

  function nowIso() {
    return new Date().toISOString();
  }

  function conceptLabel(type) {
    const found = CONCEPT_TYPES.find(function (item) { return item[0] === type; });
    return found ? found[1] : "";
  }

  function cityLabel(country, city) {
    const found = (CITIES[country] || []).find(function (item) { return item[0] === city; });
    return found ? found[1] : (city || "");
  }

  function cafeDetailUrl(cafeId, tab) {
    return "/admin-cafe?id=" + encodeURIComponent(cafeId) + (tab ? "#" + tab : "");
  }

  // page: "schedule"(출근표 스케줄 등록) | "event"(카페 이벤트 등록) | "menu"(메뉴/굿즈 등록·수정)
  function cafeSubPageUrl(page, cafeId) {
    return "/admin-cafe-" + page + "?id=" + encodeURIComponent(cafeId);
  }

  function menuItemUrl(cafeId, type, itemId) {
    return cafeSubPageUrl("menu", cafeId) + (itemId ? "&type=" + encodeURIComponent(type) + "&itemId=" + encodeURIComponent(itemId) : "");
  }

  function menuCategoryLabel(category) {
    const found = MENU_CATEGORIES.find(function (item) { return item[0] === category; });
    return found ? found[1] : category;
  }

  function formatPrice(price) {
    return Number(price || 0).toLocaleString("ko-KR") + "원";
  }

  // 메뉴(menus)와 굿즈(goods)를 함께 불러온다. 앱의 parseMenuDocument/parseGoodsDocument와 같은 기본값을 쓴다
  async function fetchMenuItems(cafeId) {
    const cafeRef = db.collection("cafes").doc(cafeId);
    const snapshots = await Promise.all([cafeRef.collection("menus").get(), cafeRef.collection("goods").get()]);
    const pickImage = function (data) {
      return normalizeText(data.image, "") || normalizeText(data.imageUrl, "") || normalizeText(data.thumbnailImage, "");
    };
    const menus = snapshots[0].docs.map(function (doc) {
      const data = doc.data() || {};
      return {
        type: "menu",
        id: doc.id,
        name: normalizeText(data.name, "이름 없음"),
        price: typeof data.price === "number" ? data.price : 0,
        desc: normalizeText(data.desc, "") || normalizeText(data.description, ""),
        image: pickImage(data),
        category: normalizeText(data.category, "drink").toLowerCase(),
        available: data.isAvailable !== false,
      };
    });
    const goods = snapshots[1].docs.map(function (doc) {
      const data = doc.data() || {};
      const stock = typeof data.stock === "number" ? data.stock : 0;
      return {
        type: "goods",
        id: doc.id,
        name: normalizeText(data.name, "이름 없음"),
        price: typeof data.price === "number" ? data.price : 0,
        desc: "",
        image: pickImage(data),
        category: "goods",
        stock: stock,
        available: stock > 0,
      };
    });
    const byName = function (a, b) { return a.name.localeCompare(b.name, "ko"); };
    return { menus: menus.sort(byName), goods: goods.sort(byName) };
  }

  function getCafeIdFromUrl() {
    return (new URLSearchParams(window.location.search).get("id") || "").trim();
  }

  // Firestore cafes 문서를 목록/상세에서 쓰는 형태로 바꾼다
  function toCafe(doc) {
    const data = doc.data() || {};
    const region = data.region || {};
    const country = normalizeText(region.country, "");
    const city = normalizeText(region.city, "");
    const address = normalizeText(region.address, "");
    return {
      id: doc.id,
      name: normalizeText(data.name, "이름 없는 카페"),
      desc: normalizeText(data.desc, ""),
      thumbnailImage: normalizeText(data.thumbnailImage, ""),
      conceptType: normalizeText(data.conceptType, ""),
      businessHours: normalizeText(data.businessHours, ""),
      phoneNumber: normalizeText(data.phoneNumber, ""),
      approved: data.approved === true,
      country: country,
      city: city,
      address: address,
      regionLabel: [city, address].filter(Boolean).join(" "),
      lat: region.location && typeof region.location.latitude === "number" ? region.location.latitude : null,
      lng: region.location && typeof region.location.longitude === "number" ? region.location.longitude : null,
      galleryImages: toStringArray(data.galleryImages),
      createdAt: normalizeText(data.createdAt, ""),
    };
  }

  function toStringArray(value) {
    return Array.isArray(value) ? value.filter(function (item) {
      return typeof item === "string" && item.trim();
    }).map(function (item) { return item.trim(); }) : [];
  }

  // Firestore cafes/{cafeId}/casts 문서를 화면용 객체로 바꾼다
  function toCast(doc) {
    const data = doc.data() || {};
    const rosterStatus = normalizeText(data.rosterStatus, "");
    return {
      id: doc.id,
      name: normalizeText(data.name, "이름 없음"),
      desc: normalizeText(data.desc, ""),
      profileImage: normalizeText(data.profileImage, ""),
      galleryImages: toStringArray(data.galleryImages),
      conceptRole: normalizeText(data.conceptRole, ""),
      birthday: normalizeText(data.birthday, ""),
      rosterStatus: rosterStatus,
      inactive: rosterStatus === CAST_ROSTER_STATUS_INACTIVE,
      inactiveAt: normalizeText(data.inactiveAt, ""),
      inactiveReason: normalizeText(data.inactiveReason, ""),
      linkedUserId: normalizeText(data.linkedUserId, ""),
      followerCount: typeof data.followerCount === "number" ? data.followerCount : 0,
      rating: typeof data.rating === "number" ? data.rating : 0,
      visitCertificationCount: typeof data.visitCertificationCount === "number" ? data.visitCertificationCount : 0,
      createdVia: normalizeText(data.createdVia, ""),
      createdAt: normalizeText(data.createdAt, ""),
      updatedAt: normalizeText(data.updatedAt, ""),
    };
  }

  // 출근표 등록이 비워 둔 소개 문구는 소개 없음으로 취급한다
  const EMPTY_CAST_DESC = "캐스트 소개가 비어있습니다.";

  function castDescText(cast) {
    return cast.desc && cast.desc !== EMPTY_CAST_DESC ? cast.desc : "";
  }

  function castRoleLabel(role) {
    const found = CAST_CONCEPT_ROLES.find(function (item) { return item[0] === role; });
    return found ? found[1] : role;
  }

  function castDetailUrl(cafeId, castId) {
    return "/admin-cast?cafeId=" + encodeURIComponent(cafeId) + "&castId=" + encodeURIComponent(castId);
  }

  async function fetchCafe(cafeId) {
    const doc = await db.collection("cafes").doc(cafeId).get();
    return doc.exists ? toCafe(doc) : null;
  }

  // 카페의 캐스트 전체를 이름순으로 불러온다 (현재 없는 캐스트 포함)
  async function fetchCasts(cafeId) {
    const snapshot = await db.collection("cafes").doc(cafeId).collection("casts").get();
    return snapshot.docs.map(toCast).sort(function (a, b) {
      return a.name.localeCompare(b.name, "ko");
    });
  }

  function cafeThumbHtml(cafe) {
    return cafe.thumbnailImage
      ? '<img src="' + escapeHtml(cafe.thumbnailImage) + '" alt="" />'
      : escapeHtml(cafe.name.slice(0, 1));
  }

  // 하위 페이지(스케줄 등록·이벤트 등록) 상단의 카페 요약과 경로 표시를 채운다
  function renderCafeMini(cafe) {
    const thumb = document.getElementById("cafeThumb");
    const title = document.getElementById("cafeTitle");
    const sub = document.getElementById("cafeSub");
    const crumb = document.getElementById("breadcrumbCafeLink");
    if (document.title.indexOf(cafe.name + " · ") !== 0) {
      document.title = cafe.name + " · " + document.title;
    }
    if (thumb) thumb.innerHTML = cafeThumbHtml(cafe);
    if (title) title.textContent = cafe.name;
    if (sub) sub.textContent = [conceptLabel(cafe.conceptType), cafe.address].filter(Boolean).join(" · ");
    if (crumb) {
      crumb.textContent = cafe.name;
      crumb.href = cafeDetailUrl(cafe.id);
    }
  }

  // 클립보드에 이미지가 있으면 onFile로 넘긴다 (isEnabled가 false면 무시)
  function bindImagePaste(isEnabled, onFile) {
    document.addEventListener("paste", function (e) {
      if (!isEnabled() || !e.clipboardData) {
        return;
      }
      const item = Array.prototype.find.call(e.clipboardData.items, function (candidate) {
        return candidate.kind === "file" && /^image\//.test(candidate.type);
      });
      if (item) {
        e.preventDefault();
        onFile(item.getAsFile());
      }
    });
  }

  function isValidClockTime(value) {
    return /^([01]\d|2[0-4]):[0-5]\d$/.test(value);
  }

  const BUSINESS_DAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];

  function everyDayBusinessHours(open, close) {
    return BUSINESS_DAY_LABELS.map(function (day) {
      return day + " " + open + "-" + close;
    }).join("\n");
  }

  function renderConceptOptions(select) {
    select.innerHTML = CONCEPT_TYPES.map(function (type) {
      return '<option value="' + type[0] + '">' + escapeHtml(type[1]) + " (" + type[0] + ")</option>";
    }).join("");
  }

  function renderCityOptions(select, country, selected) {
    const cities = (CITIES[country] || []).slice();
    if (selected && !cities.some(function (city) { return city[0] === selected; })) {
      cities.push([selected, selected]);
    }
    select.innerHTML = cities.map(function (city) {
      return '<option value="' + escapeHtml(city[0]) + '">' + escapeHtml(city[1]) + " (" + escapeHtml(city[0]) + ")</option>";
    }).join("");
    if (selected) {
      select.value = selected;
    }
  }

  // ---------- 지도 위치 선택 ----------

  function loadGoogleMaps() {
    return new Promise(function (resolve, reject) {
      if (window.google && window.google.maps) {
        resolve();
        return;
      }
      const apiKey = typeof window.CONCAFE_GOOGLE_MAPS_API_KEY === "string" ? window.CONCAFE_GOOGLE_MAPS_API_KEY.trim() : "";
      if (!apiKey) {
        reject(new Error("missing_maps_key"));
        return;
      }
      const callbackName = "__concafeAdminInitMap";
      window[callbackName] = function () {
        resolve();
        delete window[callbackName];
      };
      const script = document.createElement("script");
      script.src = "https://maps.googleapis.com/maps/api/js?key=" + encodeURIComponent(apiKey) + "&language=ko&callback=" + callbackName;
      script.async = true;
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  // 주소 → 좌표 찾기, 위도/경도 입력, 드래그 가능한 지도 핀을 묶는다
  // opts: { address, lat, lng, country, map, helper, button, onError(message) }
  function createLocationPicker(opts) {
    const DEFAULT_HELPER = "주소를 입력하고 위치 찾기를 누르세요. 지도에서 핀을 끌어 위치를 조정할 수 있습니다.";
    let map = null;
    let marker = null;

    function read() {
      const latText = opts.lat.value.trim();
      const lngText = opts.lng.value.trim();
      const lat = Number(latText);
      const lng = Number(lngText);
      if (!latText || !lngText || !isFinite(lat) || !isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
        return null;
      }
      return { lat: lat, lng: lng };
    }

    function write(position) {
      opts.lat.value = position.lat.toFixed(6);
      opts.lng.value = position.lng.toFixed(6);
    }

    async function show(position) {
      await loadGoogleMaps();
      opts.map.hidden = false;
      if (!map) {
        map = new window.google.maps.Map(opts.map, {
          center: position,
          zoom: 17,
          disableDefaultUI: true,
          zoomControl: true,
          clickableIcons: false,
        });
        marker = new window.google.maps.Marker({ position: position, map: map, draggable: true });
        marker.addListener("dragend", function () {
          const moved = marker.getPosition();
          write({ lat: moved.lat(), lng: moved.lng() });
        });
      } else {
        map.setCenter(position);
        marker.setPosition(position);
      }
    }

    async function geocode() {
      const address = opts.address.value.trim();
      if (!address) {
        opts.onError("주소를 먼저 입력하세요.");
        return;
      }
      opts.button.disabled = true;
      opts.helper.textContent = "주소로 위치를 찾는 중입니다.";
      try {
        await loadGoogleMaps();
        const geocoder = new window.google.maps.Geocoder();
        const response = await geocoder.geocode({ address: address, region: opts.country.value.toLowerCase() });
        const result = response.results && response.results[0];
        if (!result) {
          throw new Error("no_result");
        }
        const position = { lat: result.geometry.location.lat(), lng: result.geometry.location.lng() };
        write(position);
        await show(position);
        opts.helper.textContent = "찾은 위치: " + result.formatted_address + " · 핀을 끌어 정확한 위치로 조정하세요.";
      } catch (error) {
        opts.helper.textContent = "위치를 찾지 못했습니다. 주소를 바꿔 다시 시도하거나 위도/경도를 직접 입력하세요.";
        console.error(error);
      } finally {
        opts.button.disabled = false;
      }
    }

    function handleInputChange() {
      const position = read();
      if (position && map) {
        map.setCenter(position);
        marker.setPosition(position);
      }
    }

    opts.button.addEventListener("click", geocode);
    opts.lat.addEventListener("change", handleInputChange);
    opts.lng.addEventListener("change", handleInputChange);

    return {
      read: read,
      // 좌표를 채우고, 좌표가 있으면 지도에 핀을 보여준다
      set: function (position) {
        opts.helper.textContent = DEFAULT_HELPER;
        if (!position) {
          opts.lat.value = "";
          opts.lng.value = "";
          opts.map.hidden = true;
          return;
        }
        write(position);
        show(position).catch(function (error) {
          opts.map.hidden = true;
          console.error(error);
        });
      },
      reset: function () {
        opts.lat.value = "";
        opts.lng.value = "";
        opts.map.hidden = true;
        opts.helper.textContent = DEFAULT_HELPER;
      },
    };
  }

  // ---------- 이미지 편집 (업로드 대기 + 기존 URL) ----------

  // 이미지 칸 목록을 그리고, 추가/삭제를 관리한다. 저장할 때 upload()로 새 이미지를 올리고 URL 목록을 받는다.
  // opts: { container, max, shape: "portrait" | "square", addLabel, onError(message) }
  function createImageEditor(opts) {
    let items = [];
    let disabled = false;
    const inputId = "imageEditorInput" + Math.random().toString(36).slice(2, 8);

    function render() {
      const tiles = items.map(function (item, index) {
        const src = item.prepared ? item.prepared.previewUrl : item.url;
        return '<div class="image-tile ' + (opts.shape || "square") + '">' +
          '<img src="' + escapeHtml(src) + '" alt="" />' +
          (item.prepared ? '<span class="image-tile-badge">새 이미지</span>' : "") +
          '<button class="image-tile-remove" type="button" data-remove-index="' + index + '" aria-label="이미지 빼기"' + (disabled ? " disabled" : "") + ">✕</button>" +
          "</div>";
      });
      if (items.length < opts.max) {
        tiles.push('<label class="image-tile add ' + (opts.shape || "square") + '" for="' + inputId + '"' + (disabled ? ' aria-disabled="true"' : "") + ">" +
          "<span>＋</span><small>" + escapeHtml(opts.addLabel || "사진 추가") + "</small></label>");
      }
      opts.container.innerHTML = '<div class="image-editor">' + tiles.join("") + "</div>" +
        '<input id="' + inputId + '" type="file" accept="image/*"' + (opts.max - items.length > 1 ? " multiple" : "") + " hidden />" +
        '<p class="helper small">' + items.length + " / " + opts.max + "</p>";
    }

    opts.container.addEventListener("click", function (e) {
      const button = e.target.closest("[data-remove-index]");
      if (button && !disabled) {
        items.splice(Number(button.getAttribute("data-remove-index")), 1);
        render();
      }
    });
    opts.container.addEventListener("change", async function (e) {
      if (e.target.type !== "file" || disabled) {
        return;
      }
      const files = Array.prototype.slice.call(e.target.files || []);
      for (const file of files) {
        if (items.length >= opts.max) {
          opts.onError("이미지는 최대 " + opts.max + "장까지 올릴 수 있습니다.");
          break;
        }
        if (!/^image\//.test(file.type)) {
          opts.onError("이미지 파일만 올릴 수 있습니다.");
          continue;
        }
        try {
          items.push({ url: "", prepared: await prepareImage(file) });
        } catch (error) {
          opts.onError("이미지를 읽지 못했습니다. JPG나 PNG로 저장해 다시 올려주세요.");
          console.error(error);
        }
      }
      render();
    });

    render();
    return {
      setUrls: function (urls) {
        items = (urls || []).filter(Boolean).slice(0, opts.max).map(function (url) { return { url: url, prepared: null }; });
        render();
      },
      setDisabled: function (value) {
        disabled = value;
        render();
      },
      count: function () {
        return items.length;
      },
      // 새 이미지를 올리고 기존 URL과 합쳐 순서대로 돌려준다
      upload: async function (folder, ownerId) {
        const urls = [];
        for (const item of items) {
          if (item.prepared) {
            item.url = await uploadImage(folder, ownerId, item.prepared);
            item.prepared = null;
          }
          urls.push(item.url);
        }
        return urls;
      },
    };
  }

  // 더 이상 쓰지 않는 Storage 이미지를 지운다. 실패해도 저장 결과에는 영향이 없도록 오류는 기록만 한다.
  async function deleteStorageImages(urls) {
    const targets = Array.from(new Set((urls || []).filter(function (url) {
      return typeof url === "string" && url.indexOf("firebasestorage.googleapis.com") >= 0;
    })));
    await Promise.all(targets.map(function (url) {
      try {
        return storage.refFromURL(url).delete().catch(function (error) {
          console.warn("이미지 삭제 실패", url, error);
        });
      } catch (error) {
        console.warn("이미지 삭제 실패", url, error);
        return Promise.resolve();
      }
    }));
  }

  // 카페 이미지는 앱과 같이 thumbnailImage = 대표 이미지, galleryImages = [대표 이미지, ...추가 이미지]로 저장한다
  function buildCafeImageFields(representative, gallery) {
    const thumbnail = representative || null;
    const extra = (gallery || []).filter(function (url) { return url && url !== thumbnail; });
    return {
      thumbnailImage: thumbnail,
      galleryImages: (thumbnail ? [thumbnail] : []).concat(extra),
    };
  }

  // ---------- 이미지 준비 ----------

  function loadImageElement(file) {
    return new Promise(function (resolve, reject) {
      const url = URL.createObjectURL(file);
      const image = new Image();
      image.onload = function () { resolve({ image: image, url: url }); };
      image.onerror = function () {
        URL.revokeObjectURL(url);
        reject(new Error("image_decode_failed"));
      };
      image.src = url;
    });
  }

  function canvasToBlob(canvas, quality) {
    return new Promise(function (resolve) {
      canvas.toBlob(resolve, "image/jpeg", quality);
    });
  }

  // 분석 모델 이미지 입력 한도(5MB)를 넘거나 지원하지 않는 형식이면 JPEG로 다시 인코딩한다
  async function prepareImage(file) {
    const loaded = await loadImageElement(file);
    const image = loaded.image;
    const longEdge = Math.max(image.naturalWidth, image.naturalHeight);
    const needsResize = longEdge > IMAGE_MAX_EDGE;
    if (IMAGE_SUPPORTED_TYPES.indexOf(file.type) >= 0 && file.size <= IMAGE_MAX_BYTES && !needsResize) {
      return { blob: file, contentType: file.type, previewUrl: loaded.url };
    }
    let scale = needsResize ? IMAGE_MAX_EDGE / longEdge : 1;
    for (let attempt = 0; attempt < 5; attempt += 1) {
      const canvas = document.createElement("canvas");
      canvas.width = Math.round(image.naturalWidth * scale);
      canvas.height = Math.round(image.naturalHeight * scale);
      const context = canvas.getContext("2d");
      context.fillStyle = "#fff";
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      const blob = await canvasToBlob(canvas, 0.9);
      if (blob && blob.size <= IMAGE_MAX_BYTES) {
        return { blob: blob, contentType: "image/jpeg", previewUrl: loaded.url };
      }
      scale *= 0.8;
    }
    throw new Error("image_too_large");
  }

  async function uploadImage(folder, cafeId, prepared) {
    const extension = prepared.contentType.split("/")[1].replace("jpeg", "jpg");
    const objectPath = folder + "/" + cafeId + "/" + Date.now() + "-" + Math.random().toString(36).slice(2, 8) + "." + extension;
    const ref = storage.ref(objectPath);
    await ref.put(prepared.blob, { contentType: prepared.contentType });
    return ref.getDownloadURL();
  }

  // 드롭존에 파일 선택·드래그앤드롭을 연결한다
  function bindDropzone(dropzone, fileInput, onFile) {
    fileInput.addEventListener("change", function () {
      onFile(fileInput.files && fileInput.files[0]);
    });
    dropzone.addEventListener("dragover", function (e) {
      e.preventDefault();
      dropzone.classList.add("is-dragover");
    });
    dropzone.addEventListener("dragleave", function () {
      dropzone.classList.remove("is-dragover");
    });
    dropzone.addEventListener("drop", function (e) {
      e.preventDefault();
      dropzone.classList.remove("is-dragover");
      onFile(e.dataTransfer.files && e.dataTransfer.files[0]);
    });
  }

  // ---------- Cloud Functions ----------

  async function callFunction(user, name, body) {
    const idToken = await user.getIdToken();
    const response = await fetch(FUNCTIONS_BASE_URL + "/" + name, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + idToken,
      },
      body: JSON.stringify(body),
    });
    let payload = {};
    try {
      payload = await response.json();
    } catch (error) {
      payload = {};
    }
    if (!response.ok) {
      const failure = new Error(payload.error || "http_" + response.status);
      failure.code = payload.error || "";
      throw failure;
    }
    return payload;
  }

  function describeError(error) {
    return ERROR_MESSAGES[error && error.code] || "요청에 실패했습니다. 잠시 후 다시 시도하세요.";
  }

  // ---------- 로그인 / 관리자 권한 ----------

  const LOGIN_MODAL_HTML =
    '<div class="auth-modal" id="authModal" role="dialog" aria-modal="true" aria-label="로그인">' +
    '<div class="auth-modal-box">' +
    '<button class="auth-modal-close" id="authModalClose" type="button" aria-label="닫기">✕</button>' +
    '<h2 class="auth-modal-title">로그인</h2>' +
    '<form class="auth-modal-form" id="emailLoginForm" novalidate>' +
    '<div class="auth-modal-field"><label for="loginEmail">이메일</label>' +
    '<input type="email" id="loginEmail" placeholder="이메일 주소" autocomplete="email" /></div>' +
    '<div class="auth-modal-field"><label for="loginPassword">비밀번호</label>' +
    '<input type="password" id="loginPassword" placeholder="비밀번호" autocomplete="current-password" /></div>' +
    '<p class="auth-modal-error" id="emailLoginError" style="display:none;"></p>' +
    '<button class="auth-modal-submit" type="submit">로그인</button>' +
    "</form>" +
    '<div class="auth-modal-divider"><span>또는</span></div>' +
    '<button class="auth-modal-google" id="googleSignInModalBtn" type="button">' +
    '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>' +
    "Google로 로그인</button>" +
    "</div></div>";

  // 헤더 로그인 위젯(로그인 버튼·계정 표시·로그아웃)과 로그인 모달을 연결한다.
  // 로그인 상태가 바뀔 때마다 onUser(user | null)를 부른다. 권한 확인이 필요 없는 공개 페이지도 쓴다.
  function initAuthWidget(onUser) {
    document.body.insertAdjacentHTML("beforeend", LOGIN_MODAL_HTML);
    const googleProvider = new firebase.auth.GoogleAuthProvider();
    const authUserInfo = document.getElementById("authUserInfo");
    const authUserPhoto = document.getElementById("authUserPhoto");
    const authUserName = document.getElementById("authUserName");
    const signInBtn = document.getElementById("signInBtn");
    const signOutBtn = document.getElementById("signOutBtn");
    const authModal = document.getElementById("authModal");
    const authModalClose = document.getElementById("authModalClose");
    const emailLoginForm = document.getElementById("emailLoginForm");
    const emailLoginError = document.getElementById("emailLoginError");
    const googleSignInModalBtn = document.getElementById("googleSignInModalBtn");

    function openAuthModal() { authModal.classList.add("is-open"); }
    function closeAuthModal() {
      authModal.classList.remove("is-open");
      emailLoginError.style.display = "none";
      emailLoginForm.reset();
    }

    auth.onAuthStateChanged(function (user) {
      if (user) {
        authUserPhoto.src = user.photoURL || "";
        authUserPhoto.style.display = user.photoURL ? "" : "none";
        authUserName.textContent = user.displayName || user.email || "사용자";
        authUserInfo.style.display = "flex";
        signInBtn.style.display = "none";
        closeAuthModal();
      } else {
        authUserInfo.style.display = "none";
        signInBtn.style.display = "";
      }
      if (onUser) {
        onUser(user);
      }
    });

    signInBtn.addEventListener("click", openAuthModal);
    authModalClose.addEventListener("click", closeAuthModal);
    authModal.addEventListener("click", function (e) { if (e.target === authModal) closeAuthModal(); });
    document.addEventListener("keydown", function (e) { if (e.key === "Escape") closeAuthModal(); });
    emailLoginForm.addEventListener("submit", function (e) {
      e.preventDefault();
      const email = document.getElementById("loginEmail").value.trim();
      const password = document.getElementById("loginPassword").value;
      emailLoginError.style.display = "none";
      auth.signInWithEmailAndPassword(email, password).catch(function (err) {
        emailLoginError.textContent = err.code === "auth/invalid-credential" || err.code === "auth/wrong-password" || err.code === "auth/user-not-found"
          ? "이메일 또는 비밀번호가 올바르지 않습니다."
          : "로그인에 실패했습니다: " + err.message;
        emailLoginError.style.display = "block";
      });
    });
    googleSignInModalBtn.addEventListener("click", function () {
      auth.signInWithPopup(googleProvider).catch(function (err) {
        emailLoginError.textContent = "Google 로그인 실패: " + err.message;
        emailLoginError.style.display = "block";
      });
    });
    signOutBtn.addEventListener("click", function () {
      auth.signOut().then(function () { window.location.reload(); });
    });

    return { openLogin: openAuthModal };
  }

  // 관리자 페이지용: 로그인 위젯을 연결하고 관리자 여부가 바뀔 때마다 onChange({ user, isAdmin })를 부른다.
  // 로그인이 필요하거나 권한이 없으면 토스트로 알린다.
  function initAdminSession(onChange) {
    const AUTH_KEY = "authStatus";
    let widget = null;

    async function checkAdmin(user) {
      if (!user) {
        setBanner(AUTH_KEY, "info", "로그인이 필요합니다.", {
          duration: 8000,
          action: { label: "로그인", onClick: function () { widget.openLogin(); } },
        });
        onChange({ user: null, isAdmin: false });
        return;
      }
      setBanner(AUTH_KEY, "loading", "권한을 확인하는 중입니다.");
      try {
        const doc = await db.collection("users").doc(user.uid).get();
        const role = doc.exists ? doc.get("role") : "";
        if (role !== "ADMIN") {
          setBanner(AUTH_KEY, "error", "관리자 계정만 사용할 수 있습니다. (현재 계정: " + (user.email || user.uid) + ")");
          onChange({ user: null, isAdmin: false });
          return;
        }
        // 접속 성공은 헤더의 계정 표시로 충분하므로 알림을 띄우지 않는다
        hideBanner(AUTH_KEY);
        onChange({ user: user, isAdmin: true });
      } catch (error) {
        setBanner(AUTH_KEY, "error", "권한을 확인하지 못했습니다.");
        console.error(error);
        onChange({ user: null, isAdmin: false });
      }
    }

    widget = initAuthWidget(checkAdmin);
  }

  window.ConcafeAdmin = {
    app: app,
    auth: auth,
    db: db,
    storage: storage,
    CONCEPT_TYPES: CONCEPT_TYPES,
    CITIES: CITIES,
    COUNTRY_LABELS: COUNTRY_LABELS,
    CAST_ROSTER_STATUS_INACTIVE: CAST_ROSTER_STATUS_INACTIVE,
    CAST_CONCEPT_ROLES: CAST_CONCEPT_ROLES,
    cafeSubPageUrl: cafeSubPageUrl,
    MENU_CATEGORIES: MENU_CATEGORIES,
    menuItemUrl: menuItemUrl,
    menuCategoryLabel: menuCategoryLabel,
    formatPrice: formatPrice,
    fetchMenuItems: fetchMenuItems,
    getCafeIdFromUrl: getCafeIdFromUrl,
    fetchCafe: fetchCafe,
    fetchCasts: fetchCasts,
    toCast: toCast,
    castDescText: castDescText,
    castRoleLabel: castRoleLabel,
    castDetailUrl: castDetailUrl,
    isValidClockTime: isValidClockTime,
    everyDayBusinessHours: everyDayBusinessHours,
    renderConceptOptions: renderConceptOptions,
    renderCityOptions: renderCityOptions,
    createLocationPicker: createLocationPicker,
    createImageEditor: createImageEditor,
    deleteStorageImages: deleteStorageImages,
    buildCafeImageFields: buildCafeImageFields,
    cafeThumbHtml: cafeThumbHtml,
    renderCafeMini: renderCafeMini,
    bindImagePaste: bindImagePaste,
    escapeHtml: escapeHtml,
    setBanner: setBanner,
    hideBanner: hideBanner,
    normalizeText: normalizeText,
    normalizeName: normalizeName,
    nowIso: nowIso,
    conceptLabel: conceptLabel,
    cityLabel: cityLabel,
    cafeDetailUrl: cafeDetailUrl,
    toCafe: toCafe,
    prepareImage: prepareImage,
    uploadImage: uploadImage,
    bindDropzone: bindDropzone,
    callFunction: callFunction,
    describeError: describeError,
    initAuthWidget: initAuthWidget,
    initAdminSession: initAdminSession,
  };
})();
