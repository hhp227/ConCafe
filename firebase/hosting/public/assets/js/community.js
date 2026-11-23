// 커뮤니티 페이지(목록 / 게시글 / 글쓰기)가 함께 쓰는 Firestore 경로, 문서 변환, 표시 유틸리티.
// 앱(FirestoreCommunityPostRemoteDataSource)과 같은 컬렉션·필드를 쓴다. admin-common.js 다음에 로드한다.
(function () {
  const A = window.ConcafeAdmin;
  const POSTS = "communityPosts";
  const COMMENTS = "comments";
  const LIKES = "likes";
  const TITLE_MAX_LENGTH = 100;
  const CONTENT_MAX_LENGTH = 2000;
  const COMMENT_MAX_LENGTH = 500;
  const IMAGE_MAX_COUNT = 5;
  // 앱 신고 시트(community_report_type_*)와 같은 값
  const REPORT_TYPES = ["스팸/광고", "욕설/혐오", "음란물", "개인정보 노출", "기타"];

  function toNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? Math.max(0, Math.trunc(number)) : 0;
  }

  function toStringList(value) {
    return Array.isArray(value)
      ? value.filter(function (item) { return typeof item === "string" && item.trim(); })
      : [];
  }

  function toPost(doc) {
    const data = doc.data() || {};
    return {
      id: doc.id,
      userId: String(data.userId || ""),
      userNickname: String(data.userNickname || ""),
      title: String(data.title || ""),
      content: String(data.content || ""),
      imageUrls: toStringList(data.imageUrls),
      likeCount: toNumber(data.likeCount),
      commentCount: toNumber(data.commentCount),
      createdAt: String(data.createdAt || ""),
      updatedAt: String(data.updatedAt || ""),
    };
  }

  function toComment(doc) {
    const data = doc.data() || {};
    return {
      id: doc.id,
      userId: String(data.userId || ""),
      userNickname: String(data.userNickname || ""),
      content: String(data.content || ""),
      createdAt: String(data.createdAt || ""),
      updatedAt: String(data.updatedAt || ""),
    };
  }

  function pad(value) {
    return String(value).padStart(2, "0");
  }

  // 앱 displayDate와 같은 yyyy.MM.dd. withTime이면 현지 시각 HH:mm을 덧붙인다
  function formatDate(iso, withTime) {
    const date = new Date(iso);
    if (!iso || Number.isNaN(date.getTime())) {
      return String(iso || "").slice(0, 10).replace(/-/g, ".");
    }
    const day = date.getFullYear() + "." + pad(date.getMonth() + 1) + "." + pad(date.getDate());
    return withTime ? day + " " + pad(date.getHours()) + ":" + pad(date.getMinutes()) : day;
  }

  function displayName(nickname) {
    return String(nickname || "").trim() || "익명";
  }

  function avatarHtml(nickname) {
    const name = String(nickname || "").trim();
    return '<span class="community-avatar" aria-hidden="true">' + A.escapeHtml(name ? name.charAt(0) : "?") + "</span>";
  }

  // 앱(nextFirestoreEntityId)과 같은 "post-<epochMillis>" 형식
  function newEntityId(prefix) {
    return prefix + "-" + Date.now();
  }

  function postUrl(postId) {
    return "/community-post?id=" + encodeURIComponent(postId);
  }

  function writeUrl(postId) {
    return postId ? "/community-write?id=" + encodeURIComponent(postId) : "/community-write";
  }

  // 글·댓글 작성자 닉네임. 앱처럼 users 문서의 nickname을 쓰고, 없으면 로그인 계정 이름을 쓴다
  async function fetchNickname(user) {
    if (!user) {
      return "";
    }
    try {
      const doc = await A.db.collection("users").doc(user.uid).get();
      const nickname = doc.exists ? String(doc.get("nickname") || "").trim() : "";
      if (nickname) {
        return nickname;
      }
    } catch (error) {
      console.error(error);
    }
    return String(user.displayName || "").trim();
  }

  // 내가 차단한 사용자 ID 목록 (userBlocks는 차단한 본인만 읽을 수 있다)
  async function fetchBlockedUserIds(user) {
    if (!user) {
      return [];
    }
    try {
      const snapshot = await A.db.collection("userBlocks").where("blockerUserId", "==", user.uid).get();
      return snapshot.docs.map(function (doc) { return String(doc.get("blockedUserId") || ""); }).filter(Boolean);
    } catch (error) {
      console.error(error);
      return [];
    }
  }

  window.ConcafeCommunity = {
    POSTS: POSTS,
    COMMENTS: COMMENTS,
    LIKES: LIKES,
    TITLE_MAX_LENGTH: TITLE_MAX_LENGTH,
    CONTENT_MAX_LENGTH: CONTENT_MAX_LENGTH,
    COMMENT_MAX_LENGTH: COMMENT_MAX_LENGTH,
    IMAGE_MAX_COUNT: IMAGE_MAX_COUNT,
    REPORT_TYPES: REPORT_TYPES,
    postsRef: function () { return A.db.collection(POSTS); },
    toPost: toPost,
    toComment: toComment,
    formatDate: formatDate,
    displayName: displayName,
    avatarHtml: avatarHtml,
    newEntityId: newEntityId,
    postUrl: postUrl,
    writeUrl: writeUrl,
    fetchNickname: fetchNickname,
    fetchBlockedUserIds: fetchBlockedUserIds,
  };
})();
