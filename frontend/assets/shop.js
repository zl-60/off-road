const S = {
  products: [],
  cart: [],
  account: null,
  scene: "全部",
  category: "全部",
  query: "",
  epoch: 0,
};
function route() {
  return location.hash.slice(1) || "home";
}
function header() {
  const page = route();
  return (
    '<div class="top-note">为每一次出发，准备好装备。</div><header class="header"><div class="container header-inner">' +
    brand() +
    '<nav class="nav"><a href="#shop" class="' +
    (page === "shop" ? "active" : "") +
    '">装备精选</a><a href="#bundles" class="' +
    (page === "bundles" ? "active" : "") +
    '">场景套装</a><a href="#orders">我的订单</a></nav><div class="header-actions"><a class="icon-btn" href="#shop" aria-label="搜索装备">' +
    icon("search") +
    '</a><button class="icon-btn" id="account-link" aria-label="账户">' +
    icon("user") +
    '<span style="margin-left:6px;font-size:12px">' +
    esc(S.account?.username || "登录") +
    '</span></button><a class="cart-link" href="#cart" aria-label="购物车">' +
    icon("cart") +
    '<span class="cart-count">' +
    S.cart.reduce((n, l) => n + l.quantity, 0) +
    "</span></a></div></div></header>" +
    '<nav class="mobile-nav" aria-label="移动端导航">' +
    [
      ["home", "mountain", "首页"],
      ["shop", "search", "装备"],
      ["bundles", "box", "套装"],
      ["orders", "clock", "订单"],
    ]
      .map(
        ([key, symbol, label]) =>
          '<a href="#' +
          key +
          '" class="' +
          (page === key ? "active" : "") +
          '">' +
          icon(symbol, 19) +
          "<span>" +
          label +
          "</span></a>",
      )
      .join("") +
    "</nav>"
  );
}
function footer() {
  return (
    '<footer class="footer"><div class="container"><div class="footer-top">' +
    brand() +
    '<p>走进山野，让生活多一点呼吸。<br>装备有备，出发无畏。</p><a href="' +
    ADMIN_LINK +
    '" class="text-link" style="color:#c5d3c6">运营工作台 ' +
    icon("arrow", 15) +
    '</a></div><div class="footer-bottom"><span>© 山野装备 SHANYE OUTDOORS</span><span>演示商城 · 使用演示余额支付</span></div></div></footer>'
  );
}
function card(p) {
  return (
    '<article class="product-card"><div class="product-media"><a href="#" data-detail="' +
    esc(p.id) +
    '" aria-label="查看' +
    esc(p.name) +
    '"><img src="' +
    esc(p.image) +
    '" alt="' +
    esc(p.name) +
    '" loading="lazy"></a><span class="product-tag">' +
    esc(p.type === "BUNDLE" ? "场景套装" : p.scene) +
    '</span><button class="add-circle" data-add="' +
    esc(p.id) +
    '" aria-label="将' +
    esc(p.name) +
    '加入购物车" ' +
    (p.stock <= 0 ? "disabled" : "") +
    '>+</button></div><p class="product-category">' +
    esc(p.category) +
    " / " +
    esc(p.brand) +
    '</p><h3><a href="#" data-detail="' +
    esc(p.id) +
    '">' +
    esc(p.name) +
    '</a></h3><div class="product-bottom"><span class="price">' +
    money(p.price) +
    '</span><span class="stock">' +
    (p.stock > 0 ? "可售 " + p.stock + " 件" : "暂时售罄") +
    "</span></div></article>"
  );
}
function home() {
  const kit =
    S.products.find((p) => p.id === "camp-duo") || S.products.find((p) => p.type === "BUNDLE");
  return (
    '<main class="container"><section class="hero"><div><p class="eyebrow">LESS CITY. MORE WILD.</p><h1>把日常留在身后，<br><em>向山野出发。</em></h1><p class="hero-desc">从第一晚露营，到下一段山路。<br>精选可靠装备，为你的每一次出发，<br>留出更多自在的空间。</p><a class="btn primary" href="#shop">发现你的装备 ' +
    icon("arrow", 18) +
    '</a><div class="hero-meta"><span>' +
    icon("leaf", 14) +
    " 轻装探索</span><span>" +
    icon("mountain", 14) +
    ' 亲近自然</span></div></div><div class="hero-art"><img src="/assets/landscape.svg" alt="山谷营地、帐篷与远山"><span class="hero-pin">' +
    icon("mountain", 14) +
    ' 下一站，山野。</span><div class="hero-label"><span>' +
    icon("mountain", 31) +
    '</span><div>给自己一个出发的理由<small>YOUR NEXT CHAPTER STARTS OUTSIDE</small></div></div></div></section><section class="benefits"><div class="benefit">' +
    icon("check", 21) +
    '<div><strong>精选实用装备</strong><small>为露营与徒步而准备</small></div></div><div class="benefit">' +
    icon("box", 21) +
    '<div><strong>一套装备，轻松出发</strong><small>单品与场景套装随心选择</small></div></div><div class="benefit">' +
    icon("clock", 21) +
    '<div><strong>未发货可整单退款</strong><small>退款退回演示账户余额</small></div></div></section><section class="section"><div class="section-heading"><div><p class="eyebrow" style="margin-bottom:7px">CURATED FOR THE OUTDOORS</p><h2>值得带上的好装备</h2><p>让装备恰到好处，让出发轻松一点。</p></div><a class="text-link" href="#shop">查看全部 ' +
    icon("arrow", 16) +
    '</a></div><div class="product-grid">' +
    S.products
      .filter((p) => p.type === "SINGLE")
      .slice(0, 4)
      .map(card)
      .join("") +
    "</div></section>" +
    (kit
      ? '<section class="bundle-feature"><div class="bundle-image"><img src="' +
        esc(kit.image) +
        '" alt="' +
        esc(kit.name) +
        '"></div><div class="bundle-copy"><p class="eyebrow">READY. SET. CAMP.</p><h2>第一次露营，<br>从一套好装备开始。</h2><p>' +
        esc(kit.description) +
        '</p><div class="bundle-price"><span class="price">' +
        money(kit.price) +
        "</span><del>" +
        money(kit.originalPrice) +
        '</del><span class="badge">套装优惠</span></div><button class="btn primary" data-detail="' +
        esc(kit.id) +
        '">探索场景套装 ' +
        icon("arrow", 16) +
        "</button></div></section>"
      : "") +
    "</main>"
  );
}
function shop(bundles = false) {
  const types = bundles ? S.products.filter((p) => p.type === "BUNDLE") : S.products;
  const cats = ["全部", ...new Set(types.map((p) => p.category))];
  if (!cats.includes(S.category)) S.category = "全部";
  const results = types.filter(
    (p) =>
      (S.scene === "全部" || p.scene === S.scene) &&
      (S.category === "全部" || p.category === S.category) &&
      (!S.query || [p.name, p.category, p.description].some((x) => String(x).includes(S.query))),
  );
  return (
    '<main class="container"><div class="page-title"><p class="eyebrow">' +
    (bundles ? "READY FOR YOUR NEXT ADVENTURE" : "FIND YOUR OUTDOOR ESSENTIALS") +
    "</p><h2>" +
    (bundles ? "为出发准备的一整套" : "找到你的山野装备") +
    "</h2><p>" +
    (bundles ? "按场景搭配，套装与单品共享库存。" : "从一件好装备，开始下一次探索。") +
    '</p></div><div class="filters">' +
    ["全部", "露营", "徒步"]
      .map(
        (x) =>
          '<button class="chip ' +
          (S.scene === x ? "active" : "") +
          '" data-scene="' +
          x +
          '">' +
          x +
          "</button>",
      )
      .join("") +
    '<div class="search"><input id="search" placeholder="搜索装备、分类" value="' +
    esc(S.query) +
    '" aria-label="搜索装备">' +
    icon("search", 15) +
    '</div></div><div class="filters">' +
    cats
      .map(
        (x) =>
          '<button class="chip ' +
          (S.category === x ? "active" : "") +
          '" data-category="' +
          esc(x) +
          '">' +
          esc(x) +
          "</button>",
      )
      .join("") +
    '<span class="muted" style="margin-left:auto">' +
    results.length +
    ' 件装备</span></div><div class="product-grid">' +
    results.map(card).join("") +
    "</div>" +
    (results.length
      ? ""
      : empty("暂未找到装备", "换个关键词或筛选条件试试。", "#shop", "查看全部")) +
    "</main>"
  );
}
function cart() {
  if (!S.account)
    return (
      '<main class="container">' +
      empty("登录后查看购物车", "你的装备清单，会为你保留。", "#login", "登录账户") +
      "</main>"
    );
  const lines = S.cart.map((l) => ({ ...l, p: S.products.find((p) => p.id === l.productId) }));
  if (!lines.length)
    return (
      '<main class="container">' +
      empty("出发清单还是空的", "选上几件喜欢的装备，开始准备下一次出发。") +
      "</main>"
    );
  const total = lines.reduce((sum, l) => sum + (l.p?.price || 0) * l.quantity, 0);
  const valid = lines.every((l) => l.p && l.p.stock >= l.quantity);
  return (
    '<main class="container"><div class="page-title"><p class="eyebrow">YOUR NEXT ADVENTURE</p><h2>我的出发清单</h2><p>准备好装备，把更多时间留给山野。</p></div><div class="cart-layout"><div class="panel">' +
    lines
      .map(
        (l) =>
          '<div class="cart-row">' +
          (l.p
            ? '<img class="thumb" src="' + esc(l.p.image) + '" alt="' + esc(l.p.name) + '">'
            : "") +
          '<div class="cart-info"><h3>' +
          esc(l.p?.name || "商品已下架") +
          "</h3><small>" +
          esc(l.p?.category || "请移除此商品") +
          '</small></div><div class="qty"><button data-qty="' +
          esc(l.productId) +
          '" data-n="' +
          (l.quantity - 1) +
          '" aria-label="减少数量">−</button><span>' +
          l.quantity +
          '</span><button data-qty="' +
          esc(l.productId) +
          '" data-n="' +
          (l.quantity + 1) +
          '" aria-label="增加数量" ' +
          (l.quantity >= 99 ? "disabled" : "") +
          '>+</button></div><span class="price">' +
          money((l.p?.price || 0) * l.quantity) +
          '</span><button class="icon-btn" data-qty="' +
          esc(l.productId) +
          '" data-n="0" aria-label="移除">' +
          icon("close", 15) +
          "</button></div>",
      )
      .join("") +
    '</div><aside class="panel summary"><h3>订单小计</h3><div class="summary-line"><span>装备数量</span><span>' +
    lines.reduce((n, l) => n + l.quantity, 0) +
    ' 件</span></div><div class="summary-line"><span>配送费用</span><span>免运费</span></div><div class="summary-line total"><span>合计</span><strong class="price">' +
    money(total) +
    '</strong></div><p class="muted">下单后为你保留库存 15 分钟。价格以订单确认结果为准。</p>' +
    (!valid
      ? '<p class="danger-text" style="margin-top:12px;font-size:12px">清单包含下架或库存不足的装备，请调整后下单。</p>'
      : "") +
    '<button class="btn primary" id="checkout" ' +
    (!valid ? "disabled" : "") +
    ">确认出发清单 " +
    icon("arrow", 17) +
    "</button></aside></div></main>"
  );
}
function orderCard(o) {
  const lines = o.quote?.lines || [];
  return (
    '<article class="panel order-card"><div class="order-top"><div><small>订单 ' +
    esc(o.id.slice(0, 12).toUpperCase()) +
    " · " +
    date(o.createdAt) +
    "</small></div>" +
    badge(o.status) +
    '</div><div class="order-body">' +
    lines
      .map(
        (l) =>
          '<div class="order-item"><img class="thumb" src="' +
          esc(l.image) +
          '" alt="' +
          esc(l.name) +
          '"><div><h3>' +
          esc(l.name) +
          '</h3><small class="muted">数量 ' +
          l.quantity +
          '</small></div><span class="price">' +
          money(l.price * l.quantity) +
          "</span></div>",
      )
      .join("") +
    (o.lastError ? '<p class="muted">' + esc(o.lastError) + "</p>" : "") +
    (o.status === "WAITING"
      ? '<p class="muted">' + icon("clock", 13) + " 支付截止：" + date(o.expiresAt) + "</p>"
      : "") +
    (o.carrier ? '<p class="muted">' + esc(o.carrier) + " · " + esc(o.trackingNo) + "</p>" : "") +
    '<div class="order-actions"><span class="order-total">合计 <strong class="price">' +
    money(o.total) +
    '</strong></span><button class="btn small" data-order-detail="' +
    esc(o.id) +
    '">订单详情</button>' +
    (o.status === "WAITING"
      ? '<button class="btn small" data-action="cancel" data-id="' +
        o.id +
        '">取消订单</button><button class="btn primary small" data-action="pay" data-id="' +
        o.id +
        '">演示余额支付</button>'
      : "") +
    (o.status === "PAID"
      ? '<button class="btn small" data-action="refund" data-id="' +
        o.id +
        '">申请整单退款</button>'
      : "") +
    (o.status === "SHIPPED"
      ? '<button class="btn primary small" data-action="receive" data-id="' +
        o.id +
        '">确认收货</button>'
      : "") +
    "</div></div></article>"
  );
}
async function orders() {
  if (!S.account)
    return (
      '<main class="container">' +
      empty("登录后查看订单", "每一次出发，都有迹可循。", "#login", "登录账户") +
      "</main>"
    );
  const os = await api("/orders");
  return (
    '<main class="container"><div class="page-title"><p class="eyebrow">EVERY JOURNEY HAS A STORY</p><h2>我的订单</h2><p>在这里，查看装备的准备进度。</p></div>' +
    (!os.length ? empty("还没有订单", "挑选你的第一件山野装备。") : os.map(orderCard).join("")) +
    "</main>"
  );
}
async function account() {
  if (!S.account)
    return (
      '<main class="container">' +
      empty("欢迎来到山野", "登录后开始准备你的出发清单。", "#login", "登录账户") +
      "</main>"
    );
  const ledger = await api("/account/ledger");
  return (
    '<main class="container"><div class="page-title"><p class="eyebrow">HELLO, EXPLORER</p><h2>你好，' +
    esc(S.account.username) +
    '</h2></div><div class="wallet"><div><small>演示账户余额</small><strong>' +
    money(S.account.balance) +
    '</strong></div><button class="btn small" id="logout">退出登录 ' +
    icon("logout", 15) +
    '</button></div><div class="panel"><div class="section-heading"><h3>余额收支</h3><span class="muted">仅用于演示，不涉及真实资金</span></div>' +
    (ledger.length
      ? '<div class="table-wrap"><table><thead><tr><th>时间</th><th>类型</th><th>金额</th><th>订单参考</th></tr></thead><tbody>' +
        ledger
          .map(
            (l) =>
              "<tr><td>" +
              date(l.createdAt) +
              "</td><td>" +
              esc(l.kind === "DEBIT" ? "装备购买" : "整单退款") +
              '</td><td class="price">' +
              (l.delta > 0 ? "+" : "−") +
              money(Math.abs(l.delta)) +
              "</td><td>" +
              esc(l.operationId.replace(/^(pay|refund)-/, "").slice(0, 12)) +
              "</td></tr>",
          )
          .join("") +
        "</tbody></table></div>"
      : '<p class="muted">暂无余额收支记录。</p>') +
    "</div></main>"
  );
}
async function render() {
  const epoch = ++S.epoch;
  try {
    S.products = await api("/catalog/products");
    if (token) {
      try {
        S.account = await api("/account");
        S.cart = await api("/cart");
      } catch (e) {
        if (e.status === 401) {
          token = "";
          localStorage.removeItem("shop-token");
          S.account = null;
          S.cart = [];
        } else throw e;
      }
    }
    let content;
    switch (route()) {
      case "shop":
        content = shop();
        break;
      case "bundles":
        content = shop(true);
        break;
      case "cart":
        content = cart();
        break;
      case "orders":
        content = await orders();
        break;
      case "account":
        content = await account();
        break;
      default:
        content = home();
    }
    if (epoch !== S.epoch) return;
    $("#app").innerHTML = header() + content + footer();
    bind();
    if (route() === "login") login();
  } catch (e) {
    renderError(e);
  }
}
function login() {
  modal(
    '<div class="modal-heading"><p class="eyebrow">WELCOME TO SHANYE</p><h2>下一次出发，从这里开始。</h2><p>登录你的山野账户。</p></div><form id="login-form"><div class="field"><label for="username">账号</label><input id="username" name="username" autocomplete="username" required></div><div class="field"><label for="password">密码</label><input id="password" name="password" type="password" autocomplete="current-password" required></div><button class="btn primary" type="submit" style="width:100%">登录账户 ' +
      icon("arrow", 16) +
      '</button></form><div class="login-demo"><button class="btn small" id="demo-fill">填入演示账户</button><span class="muted" style="align-self:center">初始演示余额 ¥5,000</span></div>',
  );
  $("#demo-fill").onclick = () => {
    $("#username").value = "demo";
    $("#password").value = "Shanye123!";
  };
  $("#login-form").onsubmit = (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      const u = await api("/auth/login", "POST", {
        username: $("#username").value,
        password: $("#password").value,
      });
      token = u.token;
      localStorage.setItem("shop-token", token);
      closeModal();
      toast("欢迎回来，" + u.username);
      if (route() === "login") location.hash = "home";
      else await render();
    });
  };
}
function requireLogin() {
  if (!S.account) {
    login();
    return false;
  }
  return true;
}
async function add(id, button) {
  if (!requireLogin()) return;
  await busy(button, async () => {
    const current = S.cart.find((l) => l.productId === id)?.quantity || 0;
    await api("/cart", "POST", { productId: id, quantity: Math.min(99, current + 1) });
    await render();
    toast("已加入出发清单");
  });
}
function detail(id) {
  const p = S.products.find((p) => p.id === id);
  if (!p) return;
  modal(
    '<img class="detail-image" src="' +
      esc(p.image) +
      '" alt="' +
      esc(p.name) +
      '"><div class="modal-heading"><p class="eyebrow">' +
      esc(p.scene) +
      " / " +
      esc(p.category) +
      "</p><h2>" +
      esc(p.name) +
      "</h2><p>" +
      esc(p.description) +
      '</p></div><strong class="price" style="font-size:25px">' +
      money(p.price) +
      "</strong>" +
      (p.originalPrice
        ? '<del class="muted" style="margin-left:13px">' + money(p.originalPrice) + "</del>"
        : "") +
      '<div class="spec-grid">' +
      Object.entries(p.spec || {})
        .map(([k, v]) => "<div><small>" + esc(k) + "</small>" + esc(v) + "</div>")
        .join("") +
      "</div>" +
      (p.parts
        ? '<div class="parts"><strong>这套装备包含</strong>' +
          p.parts
            .map(
              (l) => "<div><span>" + esc(l.name) + "</span><span>×" + l.quantity + "</span></div>",
            )
            .join("") +
          "</div>"
        : "") +
      '<div class="form-actions"><span class="muted" style="margin-right:auto;align-self:center">' +
      (p.stock ? "可售 " + p.stock + " 件" : "暂时售罄") +
      '</span><button class="btn primary" id="detail-add" ' +
      (!p.stock ? "disabled" : "") +
      ">加入出发清单 " +
      icon("cart", 17) +
      "</button></div>",
  );
  $("#detail-add").onclick = (e) => {
    if (!requireLogin()) return;
    add(id, e.currentTarget);
  };
}
async function checkout() {
  const addresses = await api("/addresses");
  modal(
    '<div class="modal-heading"><p class="eyebrow">READY TO GO</p><h2>确认收货信息</h2><p>下单后保留库存 15 分钟，随后可使用演示余额支付。</p></div><div id="addresses">' +
      addresses
        .map(
          (a, i) =>
            '<label class="address-option"><input type="radio" name="address" value="' +
            esc(a.id) +
            '" ' +
            (i === 0 ? "checked" : "") +
            ">" +
            esc(a.recipient) +
            " · " +
            esc(a.phone) +
            "<small>" +
            esc(a.detail) +
            "</small></label>",
        )
        .join("") +
      '</div><button class="btn small" id="new-address">＋ 新增收货地址</button><form id="address-form" class="hidden" style="margin-top:20px"><div class="form-grid"><div class="field"><label>收货人</label><input name="recipient" maxlength="64" required></div><div class="field"><label>手机号</label><input name="phone" maxlength="24" required></div></div><div class="field"><label>详细地址</label><input name="detail" maxlength="500" required></div><button class="btn small" type="submit">保存地址</button></form><div class="form-actions"><button class="btn primary" id="place-order" ' +
      (!addresses.length ? "disabled" : "") +
      ">提交订单 " +
      icon("arrow", 16) +
      "</button></div>",
  );
  $("#new-address").onclick = () => $("#address-form").classList.toggle("hidden");
  $("#address-form").onsubmit = (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      await api("/addresses", "POST", Object.fromEntries(new FormData(e.target)));
      await checkout();
      toast("收货地址已保存");
    });
  };
  $("#place-order").onclick = (e) =>
    busy(e.currentTarget, async () => {
      const addressId = $('input[name="address"]:checked')?.value;
      if (!addressId) throw new Error("请选择收货地址");
      const request = {
        lines: S.cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
        addressId,
      };
      const signature = JSON.stringify(request),
        stored = JSON.parse(sessionStorage.getItem("checkout-request") || "null");
      request.requestKey = stored?.signature === signature ? stored.key : crypto.randomUUID();
      sessionStorage.setItem(
        "checkout-request",
        JSON.stringify({ signature, key: request.requestKey }),
      );
      const o = await api("/orders", "POST", request);
      if (o.status === "REJECTED") throw new Error(o.lastError || "下单未成功");
      sessionStorage.removeItem("checkout-request");
      await api("/cart", "DELETE");
      closeModal();
      location.hash = "orders";
      await render();
      toast("订单已创建，请在 15 分钟内支付");
    });
}
async function orderDetail(id) {
  const o = await api("/orders/" + id);
  modal(
    '<div class="modal-heading"><p class="eyebrow">ORDER DETAILS</p><h2>订单详情</h2><p>' +
      esc(o.id) +
      "</p></div>" +
      badge(o.status) +
      '<div class="parts">' +
      (o.quote?.lines || [])
        .map(
          (l) =>
            "<div><span>" +
            esc(l.name) +
            " ×" +
            l.quantity +
            "</span><span>" +
            money(l.price * l.quantity) +
            "</span></div>",
        )
        .join("") +
      "<div><strong>实付合计</strong><strong>" +
      money(o.total) +
      '</strong></div></div><p class="muted">收货人：' +
      esc(o.address.recipient) +
      " · " +
      esc(o.address.phone) +
      "<br>" +
      esc(o.address.detail) +
      "</p>" +
      (o.carrier
        ? '<p class="muted" style="margin-top:12px">物流：' +
          esc(o.carrier) +
          " / " +
          esc(o.trackingNo) +
          "</p>"
        : "") +
      '<div class="timeline">' +
      o.events
        .map((e) => "<div>" + esc(e.detail) + "<small>" + date(e.createdAt) + "</small></div>")
        .join("") +
      "</div>",
  );
}
function action(id, type) {
  const text = {
    pay: ["演示余额支付", "支付将从演示账户余额扣减订单金额。"],
    cancel: ["取消这次订单？", "取消后，预留的装备库存会释放。"],
    refund: ["申请整单退款？", "未发货订单将自动退款，款项退回演示账户余额。"],
    receive: ["确认收到装备？", "确认后，本次订单将完成。"],
  };
  modal(
    '<div class="modal-heading"><h2>' +
      text[type][0] +
      "</h2><p>" +
      text[type][1] +
      "</p></div>" +
      (type === "pay"
        ? '<div class="wallet"><div><small>当前演示余额</small><strong>' +
          money(S.account.balance) +
          "</strong></div></div>"
        : "") +
      '<div class="form-actions"><button class="btn" id="action-back">返回</button><button class="btn primary" id="action-confirm">确认' +
      (type === "pay" ? "支付" : type === "refund" ? "退款" : "") +
      "</button></div>",
  );
  $("#action-back").onclick = closeModal;
  $("#action-confirm").onclick = (e) =>
    busy(e.currentTarget, async () => {
      const o = await api("/orders/" + id + "/" + type, "POST", {});
      closeModal();
      await render();
      toast(statusNames[o.status] || "操作已完成");
    });
}
function bind() {
  $("#account-link").onclick = () => (S.account ? (location.hash = "account") : login());
  $$("[data-detail]").forEach(
    (b) =>
      (b.onclick = (e) => {
        e.preventDefault();
        detail(b.dataset.detail);
      }),
  );
  $$("[data-add]").forEach((b) => (b.onclick = () => add(b.dataset.add, b)));
  $$("[data-scene]").forEach(
    (b) =>
      (b.onclick = () => {
        S.scene = b.dataset.scene;
        render();
      }),
  );
  $$("[data-category]").forEach(
    (b) =>
      (b.onclick = () => {
        S.category = b.dataset.category;
        render();
      }),
  );
  if ($("#search"))
    $("#search").oninput = (e) => {
      S.query = e.target.value;
      const start = e.target.selectionStart;
      clearTimeout(window.searchTimer);
      window.searchTimer = setTimeout(async () => {
        await render();
        $("#search")?.focus();
        $("#search")?.setSelectionRange(start, start);
      }, 250);
    };
  $$("[data-qty]").forEach(
    (b) =>
      (b.onclick = () =>
        busy(b, async () => {
          await api("/cart", "POST", { productId: b.dataset.qty, quantity: Number(b.dataset.n) });
          await render();
        })),
  );
  if ($("#checkout")) $("#checkout").onclick = (e) => busy(e.currentTarget, checkout);
  $$("[data-action]").forEach((b) => (b.onclick = () => action(b.dataset.id, b.dataset.action)));
  $$("[data-order-detail]").forEach(
    (b) => (b.onclick = () => busy(b, () => orderDetail(b.dataset.orderDetail))),
  );
  if ($("#logout"))
    $("#logout").onclick = () => {
      token = "";
      localStorage.removeItem("shop-token");
      S.account = null;
      S.cart = [];
      location.hash = "home";
      render();
      toast("已退出登录");
    };
}
window.addEventListener("hashchange", () => {
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
});
setInterval(() => {
  if (route() === "orders" && token && !$("#dialog").open) render();
}, 10000);
render();
