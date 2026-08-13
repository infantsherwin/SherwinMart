// Thin fetch() wrapper shared by every page. All endpoints return the
// { success, data, error } envelope defined in Section 13 of the spec.
const Api = {
    async request(method, path, body) {
        const res = await fetch(path, {
            method,
            headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: body ? JSON.stringify(body) : undefined
        });
        const json = await res.json().catch(() => ({ success: false, error: { message: "Invalid server response" } }));
        if (!res.ok || !json.success) {
            throw new Error((json.error && json.error.message) || `Request failed (${res.status})`);
        }
        return json.data;
    },
    get(path) { return this.request("GET", path); },
    post(path, body) { return this.request("POST", path, body); },
    put(path, body) { return this.request("PUT", path, body); },
    del(path) { return this.request("DELETE", path); }
};

async function currentUser() {
    try {
        return await Api.get("api/v1/auth/me");
    } catch (e) {
        return null;
    }
}

function renderNav(activePage) {
    const nav = document.getElementById("nav");
    if (!nav) return;
    nav.innerHTML = `
        <span class="brand">SherwinMart</span>
        <a href="products.html">Browse</a>
        <a href="cart.html">Cart</a>
        <a href="orders.html">Orders</a>
        <a href="#" id="navAuthLink">Login</a>
    `;
    currentUser().then(user => {
        const link = document.getElementById("navAuthLink");
        if (user) {
            link.textContent = `Logout (${user.email})`;
            link.onclick = async (e) => {
                e.preventDefault();
                await Api.post("api/v1/auth/logout");
                window.location.href = "index.html";
            };
        } else {
            link.textContent = "Login";
            link.href = "login.html";
        }
    });
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str == null ? "" : String(str);
    return div.innerHTML;
}
