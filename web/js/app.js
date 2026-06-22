const API = {
    stats: "/api/stats",
    mahasiswa: "/api/mahasiswa",
    gedung: "/api/gedung",
    kamar: "/api/kamar",
    izin: "/api/izin",
    laporan: "/api/laporan",
    payment: "/api/payment",
    aiChat: "/api/ai/chat"
};

let mahasiswaCache = [];

const titles = {
    dashboard: ["Dashboard", "Ringkasan data sistem asrama Telkom University"],
    mahasiswa: ["Mahasiswa", "Kelola data penghuni asrama"],
    gedung: ["Gedung", "Kelola data gedung asrama"],
    kamar: ["Kamar", "Kelola kamar dan kapasitas hunian"],
    izin: ["Izin Keluar", "Pengajuan dan validasi izin keluar"],
    laporan: ["Laporan Fasilitas", "Helpdesk kerusakan sarana prasarana"],
    payment: ["Pembayaran", "Tagihan asrama dan denda"],
    ai: ["Asisten AI", "Tanyakan data asrama dengan bahasa natural"]
};

function showToast(message, isError = false) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.style.background = isError ? "#991b1b" : "#1a1f36";
    toast.classList.add("show");
    setTimeout(() => toast.classList.remove("show"), 2800);
}

async function api(url, method = "GET", body = null) {
    const options = { method, headers: { "Content-Type": "application/json" } };
    if (body) options.body = JSON.stringify(body);
    const response = await fetch(url, options);
    const text = await response.text();
    if (!response.ok) {
        let message = text || "Terjadi kesalahan";
        try {
            const err = JSON.parse(text);
            if (err.message) {
                message = err.message;
            }
        } catch (ignored) {
            // gunakan teks response mentah
        }
        throw new Error(message);
    }
    return text ? JSON.parse(text) : {};
}

function badgeClass(status) {
    if (["DISETUJUI", "LUNAS", "SELESAI"].includes(status)) return "badge success";
    if (["DITOLAK", "TERLAMBAT"].includes(status)) return "badge danger";
    if (["DIPROSES", "DIAJUKAN", "BELUM_BAYAR", "BARU"].includes(status)) return "badge warning";
    return "badge";
}

function switchSection(section) {
    document.querySelectorAll(".section").forEach(el => el.classList.remove("active"));
    document.querySelectorAll(".nav-btn").forEach(el => el.classList.remove("active"));
    document.getElementById(section).classList.add("active");
    document.querySelector(`[data-section="${section}"]`).classList.add("active");
    const [title, subtitle] = titles[section];
    document.getElementById("pageTitle").textContent = title;
    document.getElementById("pageSubtitle").textContent = subtitle;
    const topbar = document.querySelector(".topbar");
    if (topbar) {
        topbar.style.display = section === "ai" ? "none" : "";
    }
}

const statMeta = {
    mahasiswa: { label: "Mahasiswa", icon: "🎓", color: "#c8102e", sub: "Penghuni terdaftar" },
    gedung: { label: "Gedung", icon: "🏢", color: "#8b5cf6", sub: "Unit hunian" },
    kamar: { label: "Kamar", icon: "🛏️", color: "#0ea5e9", sub: "Total kamar" },
    izin: { label: "Izin Keluar", icon: "📋", color: "#f59e0b", sub: "Pengajuan izin" },
    laporan: { label: "Laporan", icon: "🔧", color: "#ef4444", sub: "Fasilitas" },
    payment: { label: "Pembayaran", icon: "💳", color: "#10b981", sub: "Tagihan" }
};

async function loadStats() {
    const stats = await api(API.stats);
    document.getElementById("statsGrid").innerHTML = Object.keys(statMeta).map(key => {
        const meta = statMeta[key];
        return `
        <div class="stat-card" style="--stat-accent: ${meta.color}">
            <span class="stat-icon">${meta.icon}</span>
            <h4>${meta.label}</h4>
            <strong>${stats[key] ?? 0}</strong>
            <div class="stat-sub">${meta.sub}</div>
        </div>`;
    }).join("");

    const heroEl = document.getElementById("heroTotalMhs");
    if (heroEl) heroEl.textContent = stats.mahasiswa ?? 0;
}

async function loadDashboardExtras() {
    try {
        const [gedung, kamar, izin, laporan] = await Promise.all([
            api(API.gedung),
            api(API.kamar),
            api(API.izin),
            api(API.laporan)
        ]);

        const chartEl = document.getElementById("occupancyChart");
        if (chartEl && gedung.length > 0) {
            const maxPenghuni = Math.max(...gedung.map(g => g.totalPenghuni || 0), 1);
            chartEl.innerHTML = gedung.map(g => {
                const pct = Math.round(((g.totalPenghuni || 0) / maxPenghuni) * 100);
                return `
                <div class="bar-row">
                    <span class="bar-label" title="${g.namaGedung}">${g.kodeGedung}</span>
                    <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
                    <span class="bar-value">${g.totalPenghuni || 0}</span>
                </div>`;
            }).join("");
        } else if (chartEl) {
            chartEl.innerHTML = `<p style="color:var(--muted);font-size:0.9rem">Belum ada data gedung.</p>`;
        }

        const activityEl = document.getElementById("recentActivity");
        if (activityEl) {
            const items = [];
            izin.slice(-3).reverse().forEach(i => items.push({
                type: "izin", icon: "📋", title: i.tujuan, sub: `Izin · ${i.status} · ${i.mahasiswaId || "-"}`
            }));
            laporan.filter(l => l.status !== "SELESAI").slice(0, 3).forEach(l => items.push({
                type: "laporan", icon: "🔧", title: l.deskripsi, sub: `Laporan · ${l.status} · ${l.lokasi}`
            }));

            activityEl.innerHTML = items.length === 0
                ? `<p style="color:var(--muted);font-size:0.9rem">Tidak ada aktivitas terbaru.</p>`
                : items.slice(0, 5).map(a => `
                <div class="activity-item">
                    <div class="activity-icon ${a.type}">${a.icon}</div>
                    <div class="activity-body">
                        <strong>${a.title}</strong>
                        <span>${a.sub}</span>
                    </div>
                </div>`).join("");
        }
    } catch (err) {
        console.warn("Dashboard extras:", err.message);
    }
}

async function loadMahasiswa() {
    mahasiswaCache = await api(API.mahasiswa);
    document.getElementById("tableMahasiswa").innerHTML = mahasiswaCache.length === 0
        ? `<tr><td colspan="9">Belum ada data mahasiswa.</td></tr>`
        : mahasiswaCache.map((item, index) => `
        <tr>
            <td>${item.id}</td>
            <td>${item.nama}</td>
            <td>${item.nim}</td>
            <td>${item.prodi || "-"}</td>
            <td>${item.noTelepon || "-"}</td>
            <td>${item.username || "-"}</td>
            <td>${item.nomorKamar || "-"}</td>
            <td>${item.namaGedung || item.kodeGedung || "-"}</td>
            <td><button type="button" class="btn btn-secondary btn-edit-mhs" data-index="${index}">Edit</button></td>
        </tr>
    `).join("");

    document.querySelectorAll(".btn-edit-mhs").forEach(btn => {
        btn.addEventListener("click", () => {
            fillMahasiswa(mahasiswaCache[btn.dataset.index]);
        });
    });
}

function fillMahasiswa(item) {
    document.getElementById("mhsId").value = item.id;
    document.getElementById("mhsNama").value = item.nama;
    document.getElementById("mhsNim").value = item.nim;
    document.getElementById("mhsProdi").value = item.prodi || "";
    document.getElementById("mhsTelepon").value = item.noTelepon || "";
    document.getElementById("mhsUsername").value = item.username || "";
    document.getElementById("mhsPassword").value = "";
    document.getElementById("mhsPassword").placeholder = "Kosongkan jika tidak diubah";
    document.getElementById("mhsKamar").value = item.nomorKamar || "";
    document.getElementById("mhsGedung").value = item.namaGedung || item.kodeGedung || "";
    switchSection("mahasiswa");
}

async function loadGedung() {
    const data = await api(API.gedung);
    document.getElementById("tableGedung").innerHTML = data.map(item => `
        <tr>
            <td>${item.kodeGedung}</td>
            <td>${item.namaGedung}</td>
            <td>${item.jenisGedung || "-"}</td>
            <td>${item.totalKamar}</td>
            <td>${item.totalPenghuni}</td>
            <td><button class="btn btn-secondary" onclick='fillGedung(${JSON.stringify(item)})'>Edit</button></td>
        </tr>
    `).join("");
}

function fillGedung(item) {
    document.getElementById("gedungKode").value = item.kodeGedung;
    document.getElementById("gedungNama").value = item.namaGedung;
    document.getElementById("gedungJenis").value = item.jenisGedung || "";
    switchSection("gedung");
}

async function loadKamar() {
    const data = await api(API.kamar);
    document.getElementById("tableKamar").innerHTML = data.length === 0
        ? `<tr><td colspan="7">Belum ada data kamar.</td></tr>`
        : data.map(item => `
        <tr>
            <td>${item.nomorKamar}</td>
            <td>${item.namaGedung || item.kodeGedung || "-"}</td>
            <td>${item.kapasitas}</td>
            <td>${item.jumlahPenghuni}</td>
            <td>${item.penghuni || "-"}</td>
            <td>${item.sisaSlot}</td>
            <td><button type="button" class="btn btn-secondary" onclick="document.getElementById('kamarNomor').value='${item.nomorKamar}'">Edit</button></td>
        </tr>
    `).join("");
}

async function loadIzin() {
    const data = await api(API.izin);
    document.getElementById("tableIzin").innerHTML = data.map(item => `
        <tr>
            <td>${item.izinId}</td>
            <td>${item.mahasiswaId || "-"}</td>
            <td>${item.tujuan}</td>
            <td><span class="${badgeClass(item.status)}">${item.status}</span></td>
            <td><button class="btn btn-secondary" onclick="document.getElementById('deleteIzinBtn').dataset.id='${item.izinId}'">Pilih Hapus</button></td>
        </tr>
    `).join("");
}

async function loadLaporan() {
    const data = await api(API.laporan);
    document.getElementById("tableLaporan").innerHTML = data.map(item => `
        <tr>
            <td>${item.reportId}</td>
            <td>${item.mahasiswaId || "-"}</td>
            <td>${item.deskripsi}</td>
            <td>${item.lokasi}</td>
            <td><span class="${badgeClass(item.status)}">${item.status}</span></td>
            <td><button class="btn btn-secondary" onclick='fillLaporan(${JSON.stringify(item)})'>Edit</button></td>
        </tr>
    `).join("");
}

function fillLaporan(item) {
    document.getElementById("laporanMhsId").value = item.mahasiswaId || "";
    document.getElementById("laporanDeskripsi").value = item.deskripsi;
    document.getElementById("laporanLokasi").value = item.lokasi;
    document.getElementById("laporanStatus").value = item.status;
    document.getElementById("deleteLaporanBtn").dataset.id = item.reportId;
    switchSection("laporan");
}

async function loadPayment() {
    const data = await api(API.payment);
    document.getElementById("tablePayment").innerHTML = data.map(item => `
        <tr>
            <td>${item.paymentId}</td>
            <td>${item.mahasiswaId || "-"}</td>
            <td>Rp ${Number(item.jumlahTagihan).toLocaleString("id-ID")}</td>
            <td>Rp ${Number(item.denda).toLocaleString("id-ID")}</td>
            <td>Rp ${Number(item.total).toLocaleString("id-ID")}</td>
            <td><span class="${badgeClass(item.status)}">${item.status}</span></td>
            <td><button class="btn btn-secondary" onclick='fillPayment(${JSON.stringify(item)})'>Edit</button></td>
        </tr>
    `).join("");
}

function fillPayment(item) {
    document.getElementById("payId").value = item.paymentId;
    document.getElementById("payMhsId").value = item.mahasiswaId || "";
    document.getElementById("payJumlah").value = item.jumlahTagihan;
    document.getElementById("payDenda").value = item.denda;
    document.getElementById("deletePaymentBtn").dataset.id = item.paymentId;
    switchSection("payment");
}

async function refreshAll(showSuccessToast = true) {
    const loaders = [
        { name: "Statistik", fn: loadStats },
        { name: "Dashboard", fn: loadDashboardExtras },
        { name: "Mahasiswa", fn: loadMahasiswa },
        { name: "Gedung", fn: loadGedung },
        { name: "Kamar", fn: loadKamar },
        { name: "Izin", fn: loadIzin },
        { name: "Laporan", fn: loadLaporan },
        { name: "Pembayaran", fn: loadPayment }
    ];

    const errors = [];
    for (const loader of loaders) {
        try {
            await loader.fn();
        } catch (err) {
            errors.push(`${loader.name}: ${err.message}`);
        }
    }

    if (errors.length > 0) {
        showToast("Gagal memuat sebagian data. " + errors.join(" | "), true);
    } else if (showSuccessToast) {
        showToast("Data berhasil dimuat ulang");
    }
}

document.querySelectorAll(".nav-btn").forEach(btn => {
    btn.addEventListener("click", () => switchSection(btn.dataset.section));
});

document.querySelectorAll("[data-goto]").forEach(btn => {
    btn.addEventListener("click", () => switchSection(btn.dataset.goto));
});

document.getElementById("refreshBtn").addEventListener("click", () => refreshAll(true));

document.getElementById("formMahasiswa").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        id: document.getElementById("mhsId").value,
        nama: document.getElementById("mhsNama").value,
        nim: document.getElementById("mhsNim").value,
        prodi: document.getElementById("mhsProdi").value,
        noTelepon: document.getElementById("mhsTelepon").value,
        username: document.getElementById("mhsUsername").value,
        password: document.getElementById("mhsPassword").value,
        nomorKamar: document.getElementById("mhsKamar").value,
        kodeGedung: document.getElementById("mhsGedung").value
    };
    try {
        const existing = await api(API.mahasiswa);
        const found = existing.find(item => item.id === payload.id);
        if (found) {
            await api(API.mahasiswa, "PUT", payload);
            showToast("Data mahasiswa diperbarui");
        } else {
            await api(API.mahasiswa, "POST", payload);
            showToast("Mahasiswa berhasil ditambahkan");
        }
        e.target.reset();
        document.getElementById("mhsPassword").placeholder = "Password";
        await refreshAll(false);
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("deleteMahasiswaBtn").addEventListener("click", async () => {
    const id = document.getElementById("mhsId").value;
    if (!id) return showToast("Isi ID mahasiswa terlebih dahulu", true);
    try {
        await api(API.mahasiswa, "DELETE", { id });
        showToast("Mahasiswa dihapus");
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("formGedung").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        kodeGedung: document.getElementById("gedungKode").value,
        namaGedung: document.getElementById("gedungNama").value,
        jenisGedung: document.getElementById("gedungJenis").value
    };
    try {
        const existing = await api(API.gedung);
        const found = existing.find(item => item.kodeGedung === payload.kodeGedung);
        if (found) {
            await api(API.gedung, "PUT", payload);
            showToast("Data gedung diperbarui");
        } else {
            await api(API.gedung, "POST", payload);
            showToast("Gedung berhasil ditambahkan");
        }
        e.target.reset();
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("deleteGedungBtn").addEventListener("click", async () => {
    const kodeGedung = document.getElementById("gedungKode").value;
    if (!kodeGedung) return showToast("Isi kode gedung terlebih dahulu", true);
    try {
        await api(API.gedung, "DELETE", { kodeGedung });
        showToast("Gedung dihapus");
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("formKamar").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        nomorKamar: document.getElementById("kamarNomor").value,
        kapasitas: document.getElementById("kamarKapasitas").value,
        kodeGedung: document.getElementById("kamarGedung").value
    };
    try {
        await api(API.kamar, "POST", payload);
        showToast("Kamar berhasil ditambahkan");
        e.target.reset();
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("deleteKamarBtn").addEventListener("click", async () => {
    const nomorKamar = document.getElementById("kamarNomor").value;
    if (!nomorKamar) return showToast("Isi nomor kamar terlebih dahulu", true);
    try {
        await api(API.kamar, "DELETE", { nomorKamar });
        showToast("Kamar dihapus");
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("formIzin").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        mahasiswaId: document.getElementById("izinMhsId").value,
        tujuan: document.getElementById("izinTujuan").value,
        jamKeluar: document.getElementById("izinJamKeluar").value,
        jamKembali: document.getElementById("izinJamKembali").value
    };
    try {
        await api(API.izin, "POST", payload);
        showToast("Izin berhasil diajukan");
        e.target.reset();
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("deleteIzinBtn").addEventListener("click", async () => {
    const izinId = document.getElementById("deleteIzinBtn").dataset.id;
    if (!izinId) return showToast("Pilih izin dari tabel terlebih dahulu", true);
    try {
        await api(API.izin, "DELETE", { izinId });
        showToast("Izin dihapus");
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("formLaporan").addEventListener("submit", async (e) => {
    e.preventDefault();
    const reportId = document.getElementById("deleteLaporanBtn").dataset.id;
    try {
        if (reportId) {
            await api(API.laporan, "PUT", {
                reportId,
                status: document.getElementById("laporanStatus").value
            });
            showToast("Status laporan diperbarui");
        } else {
            await api(API.laporan, "POST", {
                mahasiswaId: document.getElementById("laporanMhsId").value,
                deskripsi: document.getElementById("laporanDeskripsi").value,
                lokasi: document.getElementById("laporanLokasi").value
            });
            showToast("Laporan berhasil dibuat");
        }
        e.target.reset();
        delete document.getElementById("deleteLaporanBtn").dataset.id;
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("deleteLaporanBtn").addEventListener("click", async () => {
    const reportId = document.getElementById("deleteLaporanBtn").dataset.id;
    if (!reportId) return showToast("Pilih laporan dari tabel terlebih dahulu", true);
    try {
        await api(API.laporan, "DELETE", { reportId });
        showToast("Laporan dihapus");
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("formPayment").addEventListener("submit", async (e) => {
    e.preventDefault();
    const paymentId = document.getElementById("payId").value;
    const payload = {
        paymentId,
        mahasiswaId: document.getElementById("payMhsId").value,
        jumlahTagihan: document.getElementById("payJumlah").value,
        denda: document.getElementById("payDenda").value,
        prosesBayar: document.getElementById("payProses").checked ? "true" : "false"
    };
    try {
        if (paymentId) {
            await api(API.payment, "PUT", payload);
            showToast("Pembayaran diperbarui");
        } else {
            await api(API.payment, "POST", payload);
            showToast("Tagihan berhasil ditambahkan");
        }
        e.target.reset();
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

document.getElementById("deletePaymentBtn").addEventListener("click", async () => {
    const paymentId = document.getElementById("payId").value || document.getElementById("deletePaymentBtn").dataset.id;
    if (!paymentId) return showToast("Isi ID pembayaran terlebih dahulu", true);
    try {
        await api(API.payment, "DELETE", { paymentId });
        showToast("Pembayaran dihapus");
        refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

refreshAll(false).catch(err => showToast("Gagal memuat data awal: " + err.message, true));

const AI_STATUS_BADGES = {
    DISETUJUI: "success", LUNAS: "success", SELESAI: "success",
    DITOLAK: "danger", TERLAMBAT: "danger",
    DIPROSES: "warning", DIAJUKAN: "warning", BELUM_BAYAR: "warning", BARU: "warning"
};

function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function highlightStatuses(line) {
    return line.replace(
        /\b(DISETUJUI|DITOLAK|SELESAI|DIAJUKAN|DIPROSES|BARU|LUNAS|TERLAMBAT|BELUM_BAYAR)\b/g,
        (status) => `<span class="ai-inline-badge ${AI_STATUS_BADGES[status] || ""}">${status}</span>`
    );
}

function formatAiResponse(text) {
    const lines = text.split("\n");
    const parts = [];
    let listOpen = false;
    let currentItem = null;

    const applyInline = (raw) =>
        escapeHtml(raw).replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");

    const closeList = () => {
        if (currentItem) {
            parts.push(`<li class="ai-list-item">${currentItem}</li>`);
            currentItem = null;
        }
        if (listOpen) {
            parts.push("</ul>");
            listOpen = false;
        }
    };

    const openListIfNeeded = () => {
        if (!listOpen) {
            parts.push('<ul class="ai-list">');
            listOpen = true;
        }
    };

    for (const rawLine of lines) {
        const trimmed = rawLine.trim();
        if (!trimmed) {
            closeList();
            continue;
        }

        const escaped = applyInline(trimmed);

        if (/^Total(\s+ditemukan)?:/i.test(trimmed)) {
            closeList();
            parts.push(`<div class="ai-total-badge">${highlightStatuses(escaped)}</div>`);
            continue;
        }

        if (trimmed.startsWith("- ")) {
            if (currentItem) {
                parts.push(`<li class="ai-list-item">${currentItem}</li>`);
            }
            openListIfNeeded();
            currentItem = highlightStatuses(applyInline(trimmed.slice(2)));
            continue;
        }

        if (currentItem && /^\s{2,}\S/.test(rawLine)) {
            currentItem += `<br><span class="ai-sub-line">${highlightStatuses(applyInline(trimmed))}</span>`;
            continue;
        }

        if (/^[A-Za-z].+\(\d+\):$/.test(trimmed)
                || (trimmed.endsWith(":") && trimmed.length < 80 && !trimmed.startsWith("-"))) {
            closeList();
            parts.push(`<div class="ai-section-title">${highlightStatuses(escaped)}</div>`);
            continue;
        }

        closeList();
        parts.push(`<p class="ai-paragraph">${highlightStatuses(escaped)}</p>`);
    }

    closeList();
    return `<div class="ai-formatted">${parts.join("")}</div>`;
}

function setChatLoading(active) {
    const input = document.getElementById("chatInput");
    const sendBtn = document.querySelector(".chat-send-btn");
    const wrap = document.querySelector(".chat-input-wrap");
    if (input) input.disabled = active;
    if (sendBtn) sendBtn.disabled = active;
    if (wrap) wrap.classList.toggle("is-loading", active);
}

const KATA_DATA_AI = [
    "kamar", "izin", "tagihan", "statistik", "laporan", "mahasiswa", "gedung",
    "asrama", "bayar", "tampilkan", "cari", "berapa", "data", "fasilitas", "penghuni"
];

function adaIntentDataAi(text) {
    const t = text.toLowerCase();
    return KATA_DATA_AI.some((k) => t.includes(k)) || /\b(mhs\d+|u\d+)\b/i.test(t) || /\d{3,}/.test(t);
}

function isBasaBasiAi(prompt) {
    if (adaIntentDataAi(prompt)) return false;
    const t = prompt.trim().toLowerCase().replace(/[?!.,]+/g, " ").replace(/\s+/g, " ").trim();
    if (/^(apa kabar|gimana kabar|siapa kamu|kamu siapa|makasih|thanks|bye|dadah|terima kasih|lagi ngapain|lagi apa)/.test(t)) return true;
    if (/^selamat (pagi|siang|sore|malam)/.test(t)) return true;
    if (/^(halo|hallo|hello|hai|hi|hey|hei|hoi|permisi|assalamualaikum)( (kak|ka|bang|mas|mbak|min|gan))*$/.test(t)) return true;
    return false;
}

function pilihAcakAi(pilihan) {
    return pilihan[Math.floor(Math.random() * pilihan.length)];
}

function jawabBasaBasiLokal(prompt) {
    const t = prompt.toLowerCase();
    if (t.includes("makasih") || t.includes("thanks") || t.includes("terima kasih")) {
        return pilihAcakAi([
            "Sama-sama! Senang bisa membantu. Ada lagi yang mau dicek?",
            "Terima kasih kembali! Saya selalu siap bantu urusan data asrama.",
            "Sama-sama! Kabari saja kalau butuh info kamar, izin, atau tagihan lagi."
        ]);
    }
    if (t.includes("siapa kamu") || t.includes("kamu siapa")) {
        return pilihAcakAi([
            "Saya **Telyu AI**, asisten virtual **TelyuStay**.\n\nSaya bantu cari data kamar, mahasiswa, izin, laporan fasilitas, dan pembayaran langsung dari database.",
            "Perkenalkan! Saya **Telyu AI** — partner kamu mengelola data asrama.\n\nCoba tanya \"Statistik asrama\" atau \"Kamar kosong\".",
            "Nama saya **Telyu AI**. Tanya apa saja pakai bahasa natural, tanpa perlu buka banyak menu."
        ]);
    }
    if (t.includes("apa kabar") || t.includes("gimana kabar")) {
        return pilihAcakAi([
            "Baik, terima kasih! Saya siap membantu hari ini. Ada yang mau dicek dari data asrama?",
            "Alhamdulillah baik! Ada data asrama yang perlu kamu cek sekarang?",
            "Fit dan siap kerja! Mau mulai dari statistik, kamar kosong, atau izin keluar?"
        ]);
    }
    if (t.includes("bye") || t.includes("dadah") || t.includes("sampai jumpa")) {
        return pilihAcakAi([
            "Sampai jumpa! Kalau butuh info asrama lagi, saya ada di sini.",
            "Dadah! Semangat aktivitasnya — chat lagi kapan saja ya.",
            "Sampai ketemu! Telyu AI selalu on untuk bantu urusan asrama kamu."
        ]);
    }
    if (t.includes("selamat malam")) return "Selamat malam! Ada yang perlu dicek sebelum istirahat?";
    if (t.includes("selamat sore")) return "Selamat sore! Mau cek apa dari TelyuStay hari ini?";
    if (t.includes("selamat siang")) return "Selamat siang! Kamar, izin, atau laporan fasilitas — pilih saja.";
    if (t.includes("selamat pagi") || t.includes("pagi") || t.includes("assalamualaikum")) {
        return "Selamat pagi! Semoga harimu produktif. Saya Telyu AI — siap bantu data asrama.";
    }
    return pilihAcakAi([
        "Halo! Senang bertemu denganmu.\n\nSaya **Telyu AI**, asisten TelyuStay. Tanyakan apa saja tentang kamar, izin, laporan, atau tagihan.",
        "Hai! Ada yang bisa saya bantu hari ini?\n\nCoba \"Kamar kosong\" atau \"Statistik asrama\".",
        "Halo! Gimana kabarnya?\n\nSaya siap bantu — tanya saja pakai bahasa sehari-hari.",
        "Hey! Selamat datang di TelyuStay.\n\nMau mulai dari statistik asrama atau laporan fasilitas aktif?"
    ]);
}

function responsGagalDatabase(prompt, response) {
    if (response.includes("tidak menemukan data yang cocok") && isBasaBasiAi(prompt)) {
        return jawabBasaBasiLokal(prompt);
    }
    return response;
}

function addChatBubble(text, type) {
    const chatBox = document.getElementById("chatMessages");
    const row = document.createElement("div");
    row.className = `chat-row ${type}`;

    const avatar = document.createElement("div");
    avatar.className = `chat-avatar ${type === "user" ? "user-avatar" : "ai-avatar"}`;
    avatar.textContent = type === "user" ? "YOU" : "AI";

    const bubble = document.createElement("div");
    bubble.className = `chat-bubble${type === "ai" ? " ai-rich" : ""}`;
    if (type === "ai") {
        bubble.innerHTML = formatAiResponse(text);
        bubble.querySelectorAll(".ai-list-item").forEach((item, i) => {
            item.style.animationDelay = `${i * 0.04}s`;
        });
    } else {
        bubble.textContent = text;
    }

    row.appendChild(avatar);
    row.appendChild(bubble);
    chatBox.appendChild(row);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function addTypingIndicator() {
    const chatBox = document.getElementById("chatMessages");
    const row = document.createElement("div");
    row.className = "chat-row ai chat-loading";
    row.id = "chatTyping";
    row.innerHTML = `
        <div class="chat-avatar ai-avatar">AI</div>
        <div class="chat-bubble ai">
            <div class="typing-dots"><span></span><span></span><span></span></div>
        </div>`;
    chatBox.appendChild(row);
    chatBox.scrollTop = chatBox.scrollHeight;
    return row;
}

async function kirimPromptAi(prompt) {
    const trimmed = prompt.trim();
    if (!trimmed) return;

    setChatLoading(true);
    addChatBubble(trimmed, "user");
    const loading = addTypingIndicator();

    try {
        if (isBasaBasiAi(trimmed)) {
            await new Promise((r) => setTimeout(r, 400 + Math.random() * 300));
            loading.remove();
            addChatBubble(jawabBasaBasiLokal(trimmed), "ai");
            return;
        }

        const result = await api(API.aiChat, "POST", { prompt: trimmed });
        loading.remove();
        const response = responsGagalDatabase(
            trimmed,
            result.response || "Tidak ada respons dari AI."
        );
        addChatBubble(response, "ai");
    } catch (err) {
        loading.remove();
        addChatBubble("Gagal menghubungi asisten AI: " + err.message, "ai");
    } finally {
        setChatLoading(false);
        document.getElementById("chatInput")?.focus();
    }
}

document.getElementById("chatForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const input = document.getElementById("chatInput");
    const prompt = input.value;
    input.value = "";
    await kirimPromptAi(prompt);
});

document.querySelectorAll(".chip").forEach(chip => {
    chip.addEventListener("click", () => {
        kirimPromptAi(chip.dataset.prompt);
    });
});
