const A = { account: null, products: [], query: "", status: "全部" };
const tab = () => location.hash.slice(1) || "dashboard";
const titles = {
  dashboard: "运营概览",
  products: "商品与套装",
  orders: "订单管理",
  payments: "资金记录",
};
function shell(content) {
  return (
    '<div class="admin-layout"><aside class="sidebar">' +
    brand() +
    '<div class="sidebar-caption">运营工作台 / WORKSPACE</div><nav>' +
    [
      ["dashboard", "box", "运营概览"],
      ["products", "leaf", "商品与套装"],
      ["orders", "cart", "订单管理"],
      ["payments", "clock", "资金记录"],
    ]
      .map(
        ([key, i, t]) =>
          '<a href="#' +
          key +
          '" class="' +
          (tab() === key ? "active" : "") +
          '">' +
          icon(i, 18) +
          t +
          "</a>",
      )
      .join("") +
    '</nav><div class="sidebar-bottom"><a href="' +
    SHOP_LINK +
    '" target="_blank" rel="noopener">查看用户商城 ' +
    icon("arrow", 14) +
    '</a><button class="icon-btn" id="admin-logout" style="justify-content:start;padding:0">' +
    icon("logout", 14) +
    ' 退出登录</button><span>SHANYE OUTDOORS</span></div></aside><main class="admin-main"><header class="admin-header"><div><p class="eyebrow">SHANYE OPERATIONS</p><h1>' +
    esc(titles[tab()] || titles.dashboard) +
    '</h1></div><div class="admin-user"><span>把每一次出发准备好</span><div class="avatar">山</div>' +
    esc(A.account.username) +
    "</div></header>" +
    content +
    "</main></div>"
  );
}
function login() {
  token = "";
  $("#app").innerHTML =
    '<main class="admin-login">' +
    brand() +
    '<section class="panel"><div class="modal-heading"><p class="eyebrow">OPERATIONS WORKSPACE</p><h2>欢迎回到工作台</h2><p>使用管理员账户登录。</p></div><form id="admin-login"><div class="field"><label for="username">管理员账号</label><input id="username" name="username" required autocomplete="username"></div><div class="field"><label for="password">密码</label><input id="password" name="password" type="password" required autocomplete="current-password"></div><button class="btn primary" style="width:100%">登录工作台 ' +
    icon("arrow", 16) +
    '</button></form><div class="login-demo"><button class="btn small" id="admin-fill">填入演示管理员</button><a class="muted" href="' +
    SHOP_LINK +
    '" style="align-self:center">返回商城</a></div></section></main>';
  $("#admin-fill").onclick = () => {
    $("#username").value = "admin";
    $("#password").value = "ShanyeAdmin123!";
  };
  $("#admin-login").onsubmit = (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      const u = await api("/auth/login", "POST", {
        username: $("#username").value,
        password: $("#password").value,
      });
      if (u.role !== "ADMIN") throw new Error("该账户没有管理员权限");
      token = u.token;
      localStorage.setItem("admin-token", token);
      await render();
      toast("已登录运营工作台");
    });
  };
}
async function dashboard() {
  const d = await api("/admin/dashboard"),
    orders = await api("/admin/orders");
  const max = Math.max(1, ...d.trend.map((x) => x.revenue));
  const metrics = [
    ["有效销售额", money(d.revenue), "已支付、发货和完成订单"],
    ["累计订单", d.orderCount + "", "全部订单记录"],
    ["等待发货", d.pendingShipping + "", "已支付，等待装备出库"],
    ["整单退款", d.refunds + "", "款项已退回演示余额"],
  ];
  return (
    '<div class="metric-grid">' +
    metrics
      .map(
        ([title, value, note]) =>
          '<div class="metric"><small>' +
          title +
          "</small><strong>" +
          value +
          '</strong><span class="metric-note">' +
          note +
          "</span></div>",
      )
      .join("") +
    '</div><div class="dashboard-grid"><section class="panel"><div class="section-heading"><h3>近 7 日销售额</h3><span class="muted">按订单创建日期统计</span></div><div class="chart">' +
    d.trend
      .map(
        (x) =>
          '<div class="chart-col"><span>' +
          money(x.revenue) +
          '</span><div class="chart-bar" style="height:' +
          Math.max(3, (x.revenue / max) * 120) +
          'px"></div><span>' +
          x.date.slice(5) +
          "</span></div>",
      )
      .join("") +
    '</div></section><section class="panel"><h3>今天，从这里开始</h3><div class="quick-links"><a href="#orders"><span>待支付订单</span><strong>' +
    d.pendingPayment +
    '</strong></a><a href="#orders"><span>待发货订单</span><strong>' +
    d.pendingShipping +
    '</strong></a><a href="#products"><span>检查装备与库存</span>' +
    icon("arrow", 17) +
    '</a></div><p class="muted" style="margin-top:20px">统计来自真实演示订单。已退款订单不计入有效销售额。</p></section></div><section class="panel"><div class="section-heading"><h3>最近订单</h3><a class="text-link" href="#orders">查看全部 ' +
    icon("arrow", 15) +
    "</a></div>" +
    orderTable(orders.slice(0, 6)) +
    "</section>"
  );
}
function orderTable(os) {
  if (!os.length) return '<p class="muted">暂无订单。可以从用户商城完成一次购买。</p>';
  return (
    '<div class="table-wrap"><table><thead><tr><th>订单</th><th>收货人 / 装备</th><th>金额</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead><tbody>' +
    os
      .map(
        (o) =>
          "<tr><td>" +
          esc(o.id.slice(0, 10).toUpperCase()) +
          '</td><td class="wrap"><strong>' +
          esc(o.address.recipient) +
          '</strong><br><span class="muted">' +
          esc((o.quote?.lines || []).map((l) => l.name + " ×" + l.quantity).join("、")) +
          '</span></td><td class="price">' +
          money(o.total) +
          "</td><td>" +
          badge(o.status) +
          "</td><td>" +
          date(o.createdAt) +
          '</td><td><button class="btn small" data-view-order="' +
          o.id +
          '">详情</button> ' +
          (o.status === "PAID"
            ? '<button class="btn primary small" data-ship="' + o.id + '">发货</button>'
            : "") +
          "</td></tr>",
      )
      .join("") +
    "</tbody></table></div>"
  );
}
async function orders() {
  const os = await api("/admin/orders");
  const filtered = os.filter((o) => A.status === "全部" || o.status === A.status);
  return (
    '<div class="admin-toolbar"><div><h3>每一件装备，都有去向</h3><p>发货前支持自动整单退款，发货后由用户确认收货。</p></div><button class="btn small" id="refresh">刷新订单</button></div><div class="filters">' +
    ["全部", "WAITING", "PAID", "SHIPPED", "COMPLETED", "REFUNDED", "CANCELLED"]
      .map(
        (s) =>
          '<button class="chip ' +
          (A.status === s ? "active" : "") +
          '" data-status="' +
          s +
          '">' +
          (statusNames[s] || s) +
          "</button>",
      )
      .join("") +
    '</div><div class="panel">' +
    orderTable(filtered) +
    "</div>"
  );
}
async function products() {
  A.products = await api("/admin/products");
  return (
    '<div class="admin-toolbar"><div><h3>维护装备，准备下一次出发</h3><p>套装可售数量由组件库存决定，单品库存指当前可售数量。</p></div><button class="btn primary" id="new-product">＋ 新增装备 / 套装</button></div><div class="panel"><div class="table-wrap"><table><thead><tr><th>装备</th><th>类型 / 场景</th><th>售价</th><th>可售库存</th><th>预留库存</th><th>销量</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
    A.products
      .map(
        (p) =>
          '<tr><td><div style="display:flex;align-items:center;gap:12px"><img class="thumb" src="' +
          esc(p.image) +
          '" alt="' +
          esc(p.name) +
          '"><span>' +
          esc(p.name) +
          "</span></div></td><td>" +
          esc(p.type === "BUNDLE" ? "固定套装" : "单品") +
          " / " +
          esc(p.scene) +
          '</td><td class="price">' +
          money(p.price) +
          "</td><td>" +
          p.stock +
          "</td><td>" +
          (p.type === "BUNDLE" ? "见组件" : p.reserved) +
          "</td><td>" +
          p.sold +
          '</td><td><span class="badge ' +
          (p.status === "ACTIVE" ? "green" : "") +
          '">' +
          (p.status === "ACTIVE" ? "已上架" : "已下架") +
          '</span></td><td><button class="btn small" data-edit="' +
          esc(p.id) +
          '">编辑</button></td></tr>',
      )
      .join("") +
    "</tbody></table></div></div>"
  );
}
async function payments() {
  const d = await api("/admin/payments");
  return (
    '<div class="admin-toolbar"><div><h3>支付与退款记录</h3><p>所有金额均为演示余额，支付与退款各处理一次。</p></div><button class="btn small" id="refresh">刷新记录</button></div><div class="panel"><div class="table-wrap"><table><thead><tr><th>订单</th><th>演示账户</th><th>金额</th><th>支付状态</th><th>更新时间</th></tr></thead><tbody>' +
    d.payments
      .map(
        (p) =>
          "<tr><td>" +
          esc(p.orderId.slice(0, 12).toUpperCase()) +
          "</td><td>" +
          esc(p.userId) +
          '</td><td class="price">' +
          money(p.amount) +
          "</td><td>" +
          badge(p.status) +
          "</td><td>" +
          date(p.updatedAt) +
          "</td></tr>",
      )
      .join("") +
    "</tbody></table></div>" +
    (!d.payments.length ? '<p class="muted" style="padding-top:18px">暂无支付记录。</p>' : "") +
    '</div><div class="panel"><h3 style="margin-bottom:18px">订单通知记录</h3><div class="table-wrap"><table><thead><tr><th>订单</th><th>通知内容</th><th>结果</th><th>发送次数</th></tr></thead><tbody>' +
    d.events
      .map(
        (e) =>
          "<tr><td>" +
          esc(e.orderId.slice(0, 12)) +
          "</td><td>" +
          esc(e.kind === "PAID" ? "支付成功" : "退款完成") +
          '</td><td><span class="badge ' +
          (e.delivered ? "green" : "orange") +
          '">' +
          (e.delivered ? "已确认" : "等待重试") +
          "</span></td><td>" +
          e.attempts +
          "</td></tr>",
      )
      .join("") +
    "</tbody></table></div></div>"
  );
}
async function render() {
  try {
    if (!token) return login();
    A.account = await api("/account");
    if (A.account.role !== "ADMIN") {
      localStorage.removeItem("admin-token");
      return login();
    }
    const fn = { dashboard, products, orders, payments }[tab()] || dashboard;
    $("#app").innerHTML = shell(await fn());
    bind();
  } catch (e) {
    if (e.status === 401 || e.status === 403) {
      localStorage.removeItem("admin-token");
      login();
      toast(e.message);
    } else renderError(e);
  }
}
function edit(id) {
  const p = A.products.find((x) => x.id === id) || {
    id: "",
    name: "",
    type: "SINGLE",
    price: 0,
    stock: 0,
    category: "帐篷",
    scene: "露营",
    brand: "山野装备",
    description: "",
    spec: {},
    image: "/assets/tent.svg",
    status: "ACTIVE",
    parts: [],
  };
  modal(
    '<div class="modal-heading"><p class="eyebrow">PREPARE THE ESSENTIALS</p><h2>' +
      (id ? "编辑装备" : "新增装备 / 套装") +
      '</h2></div><form id="product-form"><div class="form-grid"><div class="field"><label>名称</label><input name="name" value="' +
      esc(p.name) +
      '" maxlength="120" required></div><div class="field"><label>商品类型</label><select name="type" id="product-type" ' +
      (id ? "disabled" : "") +
      '><option value="SINGLE" ' +
      (p.type === "SINGLE" ? "selected" : "") +
      '>单品</option><option value="BUNDLE" ' +
      (p.type === "BUNDLE" ? "selected" : "") +
      '>固定套装</option></select></div><div class="field"><label>售价（元）</label><input name="price" type="number" min="0.01" max="1000000" step="0.01" value="' +
      p.price / 100 +
      '" required></div><div class="field"><label>当前可售库存</label><input name="stock" id="product-stock" type="number" min="0" max="1000000" step="1" value="' +
      (p.type === "BUNDLE" ? 0 : p.stock) +
      '" ' +
      (p.type === "BUNDLE" ? "readonly" : "") +
      ' required></div><div class="field"><label>分类</label><input name="category" value="' +
      esc(p.category) +
      '" maxlength="64" required></div><div class="field"><label>使用场景</label><select name="scene"><option ' +
      (p.scene === "露营" ? "selected" : "") +
      ">露营</option><option " +
      (p.scene === "徒步" ? "selected" : "") +
      '>徒步</option></select></div><div class="field"><label>品牌</label><input name="brand" value="' +
      esc(p.brand) +
      '" maxlength="64" required></div><div class="field"><label>状态</label><select name="status"><option value="ACTIVE" ' +
      (p.status === "ACTIVE" ? "selected" : "") +
      '>上架</option><option value="INACTIVE" ' +
      (p.status === "INACTIVE" ? "selected" : "") +
      '>下架</option></select></div></div><div class="field"><label>商品图片</label><select name="image">' +
      ["tent", "chair", "lamp", "pack", "poles", "flask", "sleep", "table", "kit", "trail"]
        .map(
          (x) =>
            '<option value="/assets/' +
            x +
            '.svg" ' +
            (p.image === "/assets/" + x + ".svg" ? "selected" : "") +
            ">" +
            {
              tent: "帐篷",
              chair: "椅子",
              lamp: "营灯",
              pack: "背包",
              poles: "登山杖",
              flask: "水壶",
              sleep: "睡袋",
              table: "露营桌",
              kit: "露营套装",
              trail: "徒步套装",
            }[x] +
            "</option>",
        )
        .join("") +
      '</select></div><div class="field"><label>装备介绍</label><textarea name="description" maxlength="2000">' +
      esc(p.description) +
      '</textarea></div><div class="field"><label>规格（每行填写“属性：内容”）</label><textarea name="spec">' +
      esc(
        Object.entries(p.spec || {})
          .map(([k, v]) => k + "：" + v)
          .join("\n"),
      ) +
      '</textarea></div><section id="bundle-fields" ' +
      (p.type !== "BUNDLE" ? 'class="hidden"' : "") +
      '><h3 style="margin-bottom:12px">套装组成</h3><div id="parts"></div><button class="btn small" type="button" id="add-part">＋ 添加单品</button><p class="muted" style="margin-top:12px">套装与单品共享库存。修改套装组成不会改变已有订单。</p></section><div class="form-actions"><button class="btn primary" type="submit">保存装备 ' +
      icon("check", 16) +
      "</button></div></form>",
  );
  let parts = p.parts.map((x) => ({ itemId: x.id, quantity: x.quantity }));
  if (!parts.length)
    parts = [{ itemId: A.products.find((x) => x.type === "SINGLE")?.id || "", quantity: 1 }];
  const drawParts = () => {
    $("#parts").innerHTML = parts
      .map(
        (l, i) =>
          '<div class="part-editor"><select data-part-index="' +
          i +
          '">' +
          A.products
            .filter((x) => x.type === "SINGLE")
            .map(
              (x) =>
                '<option value="' +
                esc(x.id) +
                '" ' +
                (x.id === l.itemId ? "selected" : "") +
                ">" +
                esc(x.name) +
                "</option>",
            )
            .join("") +
          '</select><input data-part-qty="' +
          i +
          '" type="number" min="1" max="99" value="' +
          l.quantity +
          '" aria-label="组件数量"><button type="button" class="icon-btn" data-remove-part="' +
          i +
          '" aria-label="移除组件">' +
          icon("close", 15) +
          "</button></div>",
      )
      .join("");
    $$("[data-part-index]").forEach(
      (e) => (e.onchange = () => (parts[Number(e.dataset.partIndex)].itemId = e.value)),
    );
    $$("[data-part-qty]").forEach(
      (e) => (e.oninput = () => (parts[Number(e.dataset.partQty)].quantity = Number(e.value))),
    );
    $$("[data-remove-part]").forEach(
      (e) =>
        (e.onclick = () => {
          parts.splice(Number(e.dataset.removePart), 1);
          drawParts();
        }),
    );
  };
  drawParts();
  $("#add-part").onclick = () => {
    parts.push({ itemId: A.products.find((x) => x.type === "SINGLE")?.id || "", quantity: 1 });
    drawParts();
  };
  $("#product-type").onchange = (e) => {
    $("#bundle-fields").classList.toggle("hidden", e.target.value !== "BUNDLE");
    $("#product-stock").readOnly = e.target.value === "BUNDLE";
    if (e.target.value === "BUNDLE") $("#product-stock").value = 0;
  };
  $("#product-form").onsubmit = (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      const b = Object.fromEntries(new FormData(e.target));
      b.id = p.id;
      b.type = id ? p.type : b.type;
      b.price = Math.round(Number(b.price) * 100);
      b.stock = Number(b.stock);
      const spec = {};
      for (const line of b.spec.split("\n")) {
        if (!line.trim()) continue;
        const index = line.search(/[：:]/);
        if (index <= 0) throw new Error("规格请按“属性：内容”填写");
        spec[line.slice(0, index).trim()] = line.slice(index + 1).trim();
      }
      b.spec = spec;
      b.parts = parts;
      await api("/admin/products", "POST", b);
      closeModal();
      await render();
      toast("装备已保存");
    });
  };
}
async function viewOrder(id) {
  const os = await api("/admin/orders"),
    o = os.find((x) => x.id === id);
  if (!o) return;
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
            "</span><strong>" +
            money(l.price * l.quantity) +
            "</strong></div>",
        )
        .join("") +
      '</div><p class="muted">' +
      esc(o.address.recipient) +
      " · " +
      esc(o.address.phone) +
      "<br>" +
      esc(o.address.detail) +
      "</p>" +
      (o.carrier
        ? '<p style="margin-top:12px">' + esc(o.carrier) + " · " + esc(o.trackingNo) + "</p>"
        : "") +
      '<div class="timeline">' +
      o.events
        .map((e) => "<div>" + esc(e.detail) + "<small>" + date(e.createdAt) + "</small></div>")
        .join("") +
      "</div>",
  );
}
function ship(id) {
  modal(
    '<div class="modal-heading"><p class="eyebrow">READY FOR DISPATCH</p><h2>让装备出发</h2><p>确认发货后，该订单将不再支持未发货退款。</p></div><form id="ship-form"><div class="field"><label>快递公司</label><input name="carrier" maxlength="64" required placeholder="例如：顺丰速运"></div><div class="field"><label>物流单号</label><input name="trackingNo" maxlength="100" required></div><div class="form-actions"><button class="btn primary" type="submit">确认发货 ' +
      icon("box", 16) +
      "</button></div></form>",
  );
  $("#ship-form").onsubmit = (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      await api(
        "/admin/orders/" + id + "/ship",
        "POST",
        Object.fromEntries(new FormData(e.target)),
      );
      closeModal();
      await render();
      toast("已发货");
    });
  };
}
function bind() {
  $("#admin-logout").onclick = () => {
    token = "";
    localStorage.removeItem("admin-token");
    login();
  };
  if ($("#new-product")) $("#new-product").onclick = () => edit("");
  $$("[data-edit]").forEach((b) => (b.onclick = () => edit(b.dataset.edit)));
  $$("[data-view-order]").forEach(
    (b) => (b.onclick = () => busy(b, () => viewOrder(b.dataset.viewOrder))),
  );
  $$("[data-ship]").forEach((b) => (b.onclick = () => ship(b.dataset.ship)));
  $$("[data-status]").forEach(
    (b) =>
      (b.onclick = () => {
        A.status = b.dataset.status;
        render();
      }),
  );
  if ($("#refresh")) $("#refresh").onclick = (e) => busy(e.currentTarget, render);
}
window.addEventListener("hashchange", render);
setInterval(() => {
  if (token && !$("#dialog").open && ["dashboard", "orders", "payments"].includes(tab())) render();
}, 15000);
render();
