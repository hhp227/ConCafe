// 헤더의 "관리자" 메뉴를 ADMIN 계정에만 보여준다. 페이지가 Firebase 앱을 초기화한 뒤에 로드한다.
(function () {
  var link = document.getElementById("adminNavLink");
  if (!link || !window.firebase) {
    return;
  }
  var CACHE_KEY = "concafe.isAdmin";

  function show(isAdmin) {
    link.style.display = isAdmin ? "" : "none";
  }

  function remember(isAdmin) {
    try {
      sessionStorage.setItem(CACHE_KEY, isAdmin ? "1" : "0");
    } catch (error) {
      // 저장소를 쓸 수 없어도 메뉴 표시에는 영향이 없다
    }
  }

  try {
    show(sessionStorage.getItem(CACHE_KEY) === "1");
  } catch (error) {
    show(false);
  }

  function attach() {
    if (!firebase.apps || !firebase.apps.length || typeof firebase.firestore !== "function") {
      return false;
    }
    var app = firebase.apps[0];
    app.auth().onAuthStateChanged(function (user) {
      if (!user) {
        show(false);
        remember(false);
        return;
      }
      app.firestore().collection("users").doc(user.uid).get().then(function (doc) {
        var isAdmin = doc.exists && doc.get("role") === "ADMIN";
        show(isAdmin);
        remember(isAdmin);
      }).catch(function () {
        show(false);
      });
    });
    return true;
  }

  if (!attach()) {
    window.addEventListener("load", attach);
  }
})();
