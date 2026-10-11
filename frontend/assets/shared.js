const $ = (s) => document.querySelector(s),
  $$ = (s) => [...document.querySelectorAll(s)];
const esc = (v) =>
  String(v ?? "").replace(
    /[&<>"']/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c],
  );
const money = (v) =>
  "¥" +
  (Number(v || 0) / 100).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
const date = (v) => (v ? new Date(v).toLocaleString("zh-CN", { hour12: false }) : "—");
const icon = (name, size = 20) => {
  const paths = {
    mountain: '<path d="m2 19 7-13 4 7 3-5 6 11Z"/><path d="m7 10 2 3 2-3"/>',
    cart: '<path d="M2 3h3l3 12h11l3-9H6"/><circle cx="9" cy="20" r="1"/><circle cx="19" cy="20" r="1"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
    search: '<circle cx="10" cy="10" r="7"/><path d="m15 15 6 6"/>',
    arrow: '<path d="M3 12h17m-6-6 6 6-6 6"/>',
    close: '<path d="m5 5 14 14M5 19 19 5"/>',
    check: '<path d="m4 12 5 5L20 6"/>',
    box: '<path d="m3 7 9-5 9 5v10l-9 5-9-5Zm0 0 9 5 9-5M12 12v10M7 5l9 5"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>',
    leaf: '<path d="M20 3C8 1 1 9 6 16s17 2 14-13ZM5 20 16 8"/>',
    logout: '<path d="M10 3H3v18h7m4-14 5 5-5 5m-6-5h11"/>',
  };
  return (
    '<svg width="' +
    size +
    '" height="' +
    size +
    '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
    (paths[name] || paths.box) +
    "</svg>"
  );
};
const IS_ADMIN = document.body.classList.contains("admin-body");
const SHOP_LINK =
  IS_ADMIN && ["18081", "18082"].includes(location.port)
    ? location.protocol + "//" + location.hostname + ":18080/"
    : "/";
const ADMIN_LINK =
  location.port === "18080" ? location.protocol + "//" + location.hostname + ":18081/" : "/admin/";
const brand = () =>
  '<a class="brand" href="' +
  SHOP_LINK +
  '"><span class="brand-icon">' +
  icon("mountain", 28) +
  "</span><span>山野装备<small>SHANYE OUTDOORS</small></span></a>";
let token = localStorage.getItem(IS_ADMIN ? "admin-token" : "shop-token") || "";
async function api(path, method = "GET", body) {
  const r = await fetch("/api" + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const data = await r.json().catch(() => ({ message: "服务暂时无法响应" }));
  if (!r.ok) {
    const e = new Error(data.message || "操作失败");
    e.status = r.status;
    throw e;
  }
  return data;
}
function toast(text) {
  $("#toast").textContent = text;
  $("#toast").classList.add("show");
  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => $("#toast").classList.remove("show"), 3500);
}
function modal(html) {
  const d = $("#dialog");
  d.innerHTML =
    '<button class="modal-close icon-btn" aria-label="关闭">' + icon("close") + "</button>" + html;
  d.querySelector(".modal-close").onclick = () => d.close();
  d.onclick = (e) => {
    if (e.target === d) {
      const r = d.getBoundingClientRect();
      if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom)
        d.close();
    }
  };
  if (!d.open) d.showModal();
}
const closeModal = () => $("#dialog").close();
async function busy(button, fn) {
  if (button.disabled) return;
  button.disabled = true;
  const prev = button.innerHTML;
  try {
    await fn();
  } catch (e) {
    toast(e.message);
  } finally {
    if (button.isConnected) {
      button.disabled = false;
      button.innerHTML = prev;
    }
  }
}
const statusNames = {
  CREATING: "订单确认中",
  WAITING: "待支付",
  PAYING: "支付处理中",
  PAID: "待发货",
  SHIPPED: "待收货",
  COMPLETED: "已完成",
  CANCELLING: "取消处理中",
  CANCELLED: "已取消",
  REFUNDING: "退款处理中",
  REFUNDED: "已退款",
  REJECTED: "下单未成功",
};
const badge = (s) =>
  '<span class="badge ' +
  (s === "PAID" || s === "COMPLETED" ? "green" : s === "WAITING" ? "orange" : "") +
  '">' +
  esc(statusNames[s] || s) +
  "</span>";
function empty(title, body, link = "#shop", label = "去逛逛") {
  return (
    '<div class="empty">' +
    icon("mountain", 48) +
    "<h2>" +
    esc(title) +
    "</h2><p>" +
    esc(body) +
    '</p><a class="btn primary" href="' +
    link +
    '">' +
    label +
    "</a></div>"
  );
}
function renderError(e) {
  $("#app").innerHTML =
    '<main class="container">' +
    empty("暂时无法加载", e.message, location.href, "重新加载") +
    "</main>";
}
