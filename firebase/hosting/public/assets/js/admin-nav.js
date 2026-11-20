// 헤더 운영 메뉴(.app-nav)를 관리한다. 페이지가 Firebase 앱을 초기화한 뒤에 로드한다.
// 1) 현재 주소에 해당하는 메뉴에 선택 표시(active-page)를 붙인다. 카페 상세·관리 하위 페이지는 '카페 목록'으로 표시한다.
// 2) 권한이 필요한 항목을 사용자 role에 맞춰 보여준다.
//    - id="adminNavLink"            → ADMIN에게만 표시
//    - data-nav-roles="ADMIN,CAFE_OWNER" → 나열한 role 중 하나일 때만 표시
// 메뉴 표시만 담당하며, 실제 접근 권한은 각 페이지와 Firestore 규칙이 확인한다.
(function () {
  function normalizePath(path) {
    var normalized = String(path || "/").replace(/\.html$/, "").replace(/\/+$/, "");
    return normalized || "/";
  }

  var currentPath = normalizePath(window.location.pathname);
  // 카페 상세와 그 안에서 들어가는 스케줄 조회·등록, 캐스트 등록, 관리자 하위 화면(/admin-*)은 '카페 목록'으로 표시한다
  if (currentPath.indexOf("/admin") === 0 || ["/cafe", "/cast", "/schedule", "/registration", "/cast-new"].indexOf(currentPath) >= 0) {
    currentPath = "/cafes";
  }
  Array.prototype.forEach.call(document.querySelectorAll(".app-nav .nav-link"), function (link) {
    var active = normalizePath(link.getAttribute("href")) === currentPath;
    link.classList.toggle("active-page", active);
    if (active) {
      link.setAttribute("aria-current", "page");
    } else {
      link.removeAttribute("aria-current");
    }
  });
})();

(function () {
  var CACHE_KEY = "concafe.userRole";
  var items = [];

  var adminLink = document.getElementById("adminNavLink");
  if (adminLink) {
    items.push({ el: adminLink, roles: ["ADMIN"] });
  }
  Array.prototype.forEach.call(document.querySelectorAll("[data-nav-roles]"), function (el) {
    var roles = el.getAttribute("data-nav-roles").split(",").map(function (role) {
      return role.trim();
    }).filter(Boolean);
    items.push({ el: el, roles: roles });
  });

  if (!items.length || !window.firebase) {
    return;
  }

  function apply(role) {
    items.forEach(function (item) {
      item.el.style.display = role && item.roles.indexOf(role) >= 0 ? "" : "none";
    });
  }

  function remember(role) {
    try {
      sessionStorage.setItem(CACHE_KEY, role || "");
    } catch (error) {
      // 저장소를 쓸 수 없어도 메뉴 표시에는 영향이 없다
    }
  }

  // 페이지 이동 시 메뉴가 깜빡이지 않도록 이전에 확인한 role로 먼저 그린다
  try {
    apply(sessionStorage.getItem(CACHE_KEY) || "");
  } catch (error) {
    apply("");
  }

  function attach() {
    if (!firebase.apps || !firebase.apps.length || typeof firebase.firestore !== "function") {
      return false;
    }
    var app = firebase.apps[0];
    app.auth().onAuthStateChanged(function (user) {
      if (!user) {
        apply("");
        remember("");
        return;
      }
      app.firestore().collection("users").doc(user.uid).get().then(function (doc) {
        var role = doc.exists && typeof doc.get("role") === "string" ? doc.get("role") : "";
        apply(role);
        remember(role);
      }).catch(function () {
        apply("");
      });
    });
    return true;
  }

  if (!attach()) {
    window.addEventListener("load", attach);
  }
})();
