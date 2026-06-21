function showToast(text) {
    let toast = document.getElementById('toast');
    let toastNickname = document.getElementById('toast-text');
    toastNickname.textContent = text;
    toast.style.display = 'block';
    setTimeout(function () {
        toast.style.display = 'none';
    }, 5000);
}

function buildUrl(path) {
    const base = window.baseUrl || '/';
    const baseNormalized = base.endsWith('/') ? base : base + '/';
    const pathNormalized = path.startsWith('/') ? path.slice(1) : path;
    return baseNormalized + pathNormalized;
}

function csrfHeaders(headers) {
    const result = Object.assign({}, headers || {});
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header && token.content && header.content) {
        result[header.content] = token.content;
    }
    return result;
}

let adjustmentsCache = null;
let historyCache = new Map();
let historyRequests = new Map();
let selectedIds = new Set();
let currentUserId = null;
let currentFavorite = 0;
let attendanceUsers = [];
let attendanceSelectedIds = new Set();
let attendanceDeselectedIds = new Set();
let attendanceSelectionInitialized = false;
let attendancePollingId = null;
let attendanceCaptureActive = false;
let rouletteTables = [];
let selectedRouletteTableId = null;
let rouletteEventPage = 0;
const ROULETTE_EVENT_PAGE_SIZE = 5;

function getUiPermissions() {
    const root = document.body;
    if (!root) {
        return {isAdmin: false, currentUserId: null};
    }
    const rawUserId = root.dataset.currentUserId;
    return {
        isAdmin: root.dataset.isAdmin === 'true',
        currentUserId: rawUserId && rawUserId !== 'null' ? rawUserId : null
    };
}

function requireAdminPermission() {
    if (getUiPermissions().isAdmin) {
        return true;
    }
    showToast('권한이 없습니다.');
    return false;
}

function canViewHistory(userId) {
    const permissions = getUiPermissions();
    if (permissions.isAdmin) {
        return true;
    }
    return Boolean(permissions.currentUserId && permissions.currentUserId === userId);
}

function openModal(row) {
    if (!requireAdminPermission()) {
        return;
    }
    currentUserId = row.getAttribute('data-user-id');
    currentFavorite = parseInt(row.getAttribute('data-favorite') || '0', 10);
    const nickName = row.getAttribute('data-nick-name') || '';
    document.getElementById('karma-target').textContent = nickName || currentUserId;
    const calcCurrent = document.getElementById('calc-current');
    const calcManual = document.getElementById('calc-manual');
    const calcDelta = document.getElementById('calc-delta');
    const calcAfter = document.getElementById('calc-after');
    if (calcCurrent) {
        calcCurrent.textContent = currentFavorite;
    }
    if (calcManual) {
        calcManual.textContent = '0';
    }
    if (calcDelta) {
        calcDelta.textContent = '0';
    }
    if (calcAfter) {
        calcAfter.textContent = currentFavorite;
    }
    selectedIds.clear();
    const manualAmount = document.getElementById('manual-amount');
    const manualHistory = document.getElementById('manual-history');
    if (manualAmount) {
        manualAmount.value = '';
    }
    if (manualHistory) {
        manualHistory.value = '';
    }
    loadAdjustments().then(function () {
        document.getElementById('karma-modal').style.display = 'flex';
    });
}

function closeModal() {
    document.getElementById('karma-modal').style.display = 'none';
}

function loadHistory(userId, limit) {
    if (historyCache.has(userId)) {
        return Promise.resolve(historyCache.get(userId));
    }
    if (historyRequests.has(userId)) {
        return historyRequests.get(userId);
    }
    const safeLimit = typeof limit === 'number' ? limit : 10;
    const request = fetch(buildUrl('/favorite/history?userId=' + encodeURIComponent(userId) + '&limit=' + safeLimit))
        .then(function (response) {
            if (response.status === 403) {
                throw new Error('FORBIDDEN');
            }
            if (!response.ok) {
                throw new Error('Failed to load history');
            }
            return response.json();
        })
        .then(function (data) {
            const items = Array.isArray(data) ? data : [];
            historyCache.set(userId, items);
            historyRequests.delete(userId);
            return items;
        })
        .catch(function (error) {
            historyRequests.delete(userId);
            throw error;
        });
    historyRequests.set(userId, request);
    return request;
}

function buildHistoryCell(className, text) {
    const cell = document.createElement('div');
    cell.className = 'history-cell' + (className ? ' ' + className : '');
    cell.textContent = text;
    return cell;
}

function renderHistory(row, items) {
    const grid = row.querySelector('.history-grid');
    if (!grid) {
        return;
    }
    grid.innerHTML = '';
    if (!items || items.length === 0) {
        const emptyRow = document.createElement('div');
        emptyRow.className = 'history-row';
        emptyRow.appendChild(buildHistoryCell('score', '-'));
        emptyRow.appendChild(buildHistoryCell('reason', '히스토리 없음'));
        emptyRow.appendChild(buildHistoryCell('date', '-'));
        grid.appendChild(emptyRow);
    } else {
        items.forEach(function (item) {
            const rowEl = document.createElement('div');
            rowEl.className = 'history-row';
            rowEl.appendChild(buildHistoryCell('score', item.favorite != null ? String(item.favorite) : '-'));
            rowEl.appendChild(buildHistoryCell('reason', item.history || '-'));
            rowEl.appendChild(buildHistoryCell('date', item.date || '-'));
            grid.appendChild(rowEl);
        });
    }
    row.dataset.historyLoaded = 'true';
    row.dataset.historyLoading = 'false';
}

function showHistoryLoading(row) {
    const grid = row.querySelector('.history-grid');
    if (!grid) {
        return;
    }
    grid.innerHTML = '';
    const loadingRow = document.createElement('div');
    loadingRow.className = 'history-row';
    loadingRow.appendChild(buildHistoryCell('score', ''));
    loadingRow.appendChild(buildHistoryCell('reason', '불러오는 중...'));
    loadingRow.appendChild(buildHistoryCell('date', ''));
    grid.appendChild(loadingRow);
}

function loadHistoryForRow(row) {
    if (!row) {
        return;
    }
    const userId = row.getAttribute('data-user-id');
    if (!userId || !canViewHistory(userId)) {
        showToast('권한이 없습니다.');
        return;
    }
    if (row.dataset.historyLoaded === 'true' || row.dataset.historyLoading === 'true') {
        return;
    }
    row.dataset.historyLoading = 'true';
    showHistoryLoading(row);
    loadHistory(userId, 10)
        .then(function (items) {
            renderHistory(row, items);
        })
        .catch(function (error) {
            row.dataset.historyLoading = 'false';
            const details = row.querySelector('.history-details');
            if (details) {
                details.style.display = 'none';
            }
            if (error.message === 'FORBIDDEN') {
                showToast('권한이 없습니다.');
                return;
            }
            showToast('히스토리를 불러오지 못했습니다.');
        });
}

function startAttendanceManagement() {
    if (!requireAdminPermission()) {
        return;
    }
    if (attendanceCaptureActive) {
        return;
    }
    attendanceCaptureActive = true;
    const amountInput = document.getElementById('attendance-amount');
    if (amountInput && !attendanceSelectionInitialized) {
        amountInput.value = '1';
    }
    startAttendanceCapture()
        .then(function () {
            if (!attendanceCaptureActive) {
                return;
            }
            fetchAttendanceUsers();
            startAttendancePolling();
        })
        .catch(function () {
            attendanceCaptureActive = false;
            showToast('권한이 없습니다.');
        });
}

function stopAttendanceManagement() {
    if (!attendanceCaptureActive) {
        return;
    }
    attendanceCaptureActive = false;
    stopAttendancePolling();
    stopAttendanceCapture();
}

function startAttendancePolling() {
    stopAttendancePolling();
    attendancePollingId = setInterval(fetchAttendanceUsers, 5000);
}

function stopAttendancePolling() {
    if (attendancePollingId) {
        clearInterval(attendancePollingId);
        attendancePollingId = null;
    }
}

function startAttendanceCapture() {
    return fetch(buildUrl('/attendance/start'), {
        method: 'POST',
        headers: csrfHeaders()
    }).then(function (response) {
        if (!response.ok) {
            throw new Error('Failed to start attendance');
        }
    });
}

function stopAttendanceCapture() {
    return fetch(buildUrl('/attendance/stop'), {
        method: 'POST',
        headers: csrfHeaders()
    });
}

function fetchAttendanceUsers() {
    return fetch(buildUrl('/attendance/users'))
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Failed to load attendance users');
            }
            return response.json();
        })
        .then(function (data) {
            const previousIds = new Set(attendanceUsers.map(function (user) {
                return user.userId;
            }));
            const nextUsers = Array.isArray(data) ? data : [];
            const availableIds = new Set(nextUsers.map(function (user) {
                return user.userId;
            }));
            attendanceUsers = nextUsers;
            if (!attendanceSelectionInitialized) {
                attendanceSelectedIds = new Set(nextUsers.map(function (user) {
                    return user.userId;
                }));
                attendanceSelectionInitialized = true;
            } else {
                const preservedSelectedIds = new Set();
                attendanceSelectedIds.forEach(function (userId) {
                    if (availableIds.has(userId)) {
                        preservedSelectedIds.add(userId);
                    }
                });
                nextUsers.forEach(function (user) {
                    if (!previousIds.has(user.userId) && !attendanceDeselectedIds.has(user.userId)) {
                        preservedSelectedIds.add(user.userId);
                    }
                });
                attendanceSelectedIds = preservedSelectedIds;
            }
            renderAttendanceUsers();
        })
        .catch(function () {
            showToast('현재 채팅 사용자 목록을 불러오지 못했습니다.');
        });
}

function renderAttendanceUsers() {
    const list = document.getElementById('attendance-list');
    if (!list) {
        return;
    }
    list.innerHTML = '';
    if (!attendanceUsers || attendanceUsers.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'attendance-empty';
        empty.textContent = '현재 채팅 참여자가 없습니다.';
        list.appendChild(empty);
        updateAttendanceStatus();
        return;
    }
    attendanceUsers.forEach(function (user) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'attendance-user-button';
        if (attendanceSelectedIds.has(user.userId)) {
            button.classList.add('is-selected');
        }
        button.textContent = user.nickName || user.userId;
        button.addEventListener('click', function (event) {
            event.preventDefault();
            toggleAttendanceSelection(user.userId, button);
        });
        list.appendChild(button);
    });
    updateAttendanceStatus();
}

function toggleAttendanceSelection(userId, button) {
    if (attendanceSelectedIds.has(userId)) {
        attendanceSelectedIds.delete(userId);
        attendanceDeselectedIds.add(userId);
        button.classList.remove('is-selected');
    } else {
        attendanceSelectedIds.add(userId);
        attendanceDeselectedIds.delete(userId);
        button.classList.add('is-selected');
    }
    updateAttendanceStatus();
}

function updateAttendanceStatus() {
    const countEl = document.getElementById('attendance-count');
    const totalEl = document.getElementById('attendance-total');
    if (countEl) {
        countEl.textContent = attendanceSelectedIds.size + '명 선택';
    }
    if (totalEl) {
        totalEl.textContent = (attendanceUsers ? attendanceUsers.length : 0) + '명 현재';
    }
}

function applyAttendance() {
    if (!requireAdminPermission()) {
        return;
    }
    const amountInput = document.getElementById('attendance-amount');
    const rawAmount = amountInput ? amountInput.value.trim() : '1';
    const amount = parseInt(rawAmount, 10);
    if (Number.isNaN(amount) || amount <= 0) {
        showToast('1 이상의 값을 입력해 주세요.');
        return;
    }

    const selectedUsers = attendanceUsers.filter(function (user) {
        return attendanceSelectedIds.has(user.userId);
    });
    if (selectedUsers.length === 0) {
        showToast('출석체크 대상을 선택해 주세요.');
        return;
    }

    fetch(buildUrl('/attendance/apply'), {
        method: 'POST',
        headers: csrfHeaders({'Content-Type': 'application/json'}),
        body: JSON.stringify({
            amount: amount,
            users: selectedUsers.map(function (user) {
                return {userId: user.userId, nickName: user.nickName};
            })
        })
    })
        .then(function (response) {
            if (response.status === 403) {
                throw new Error('FORBIDDEN');
            }
            if (!response.ok) {
                throw new Error('Failed to apply attendance');
            }
            return response.json();
        })
        .then(function () {
            showToast('출석체크 완료');
            stopAttendanceManagement();
            setTimeout(function () {
                window.location.reload();
            }, 400);
        })
        .catch(function (error) {
            if (error.message === 'FORBIDDEN') {
                showToast('권한이 없습니다.');
                return;
            }
            showToast('출석체크에 실패했습니다.');
        });
}

function loadRouletteManagement() {
    if (!requireAdminPermission()) {
        return;
    }
    loadRouletteTables();
    loadRouletteEvents(rouletteEventPage);
}

function loadRouletteTables() {
    return fetch(buildUrl('/admin/roulette/tables'))
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Failed to load roulette tables');
            }
            return response.json();
        })
        .then(function (data) {
            rouletteTables = Array.isArray(data) ? data : [];
            if (!selectedRouletteTableId && rouletteTables.length > 0) {
                selectedRouletteTableId = rouletteTables[0].id;
            }
            if (selectedRouletteTableId && !rouletteTables.some(function (table) {
                return table.id === selectedRouletteTableId;
            })) {
                selectedRouletteTableId = rouletteTables.length > 0 ? rouletteTables[0].id : null;
            }
            renderRouletteTables();
            renderSelectedRouletteTable();
        })
        .catch(function () {
            showToast('룰렛 설정을 불러오지 못했습니다.');
        });
}

function loadRouletteEvents(page) {
    if (!getUiPermissions().isAdmin) {
        return Promise.resolve();
    }
    const safePage = Math.max(page || 0, 0);
    return fetch(buildUrl('/admin/roulette/events?page=' + safePage + '&size=' + ROULETTE_EVENT_PAGE_SIZE))
        .then(requireOk)
        .then(function (data) {
            rouletteEventPage = data.number || 0;
            renderRouletteEvents(data);
        })
        .catch(function () {
            showToast('룰렛 실행 목록을 불러오지 못했습니다.');
        });
}

function getSelectedRouletteTable() {
    return rouletteTables.find(function (table) {
        return table.id === selectedRouletteTableId;
    }) || null;
}

function renderRouletteTables() {
    const list = document.getElementById('roulette-table-list');
    if (!list) {
        return;
    }
    list.innerHTML = '';
    if (rouletteTables.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'roulette-empty';
        empty.textContent = '등록된 룰렛이 없습니다.';
        list.appendChild(empty);
        return;
    }
    rouletteTables.forEach(function (table) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'roulette-table-button';
        if (table.id === selectedRouletteTableId) {
            button.classList.add('is-selected');
        }
        if (table.active) {
            button.classList.add('is-active');
        }
        const title = document.createElement('span');
        title.textContent = table.title || ('룰렛 #' + table.id);
        const meta = document.createElement('strong');
        meta.textContent = (table.command || '-') + ' · ' + formatNumber(table.pricePerRound || 0);
        button.appendChild(title);
        button.appendChild(meta);
        button.addEventListener('click', function (event) {
            event.preventDefault();
            selectedRouletteTableId = table.id;
            renderRouletteTables();
            renderSelectedRouletteTable();
        });
        list.appendChild(button);
    });
}

function renderSelectedRouletteTable() {
    const table = getSelectedRouletteTable();
    const itemList = document.getElementById('roulette-item-list');
    if (!itemList) {
        return;
    }
    itemList.innerHTML = '';
    if (!table) {
        updateRouletteStatus(null);
        return;
    }
    const items = Array.isArray(table.items) ? table.items : [];
    if (items.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'roulette-empty';
        empty.textContent = '등록된 항목이 없습니다.';
        itemList.appendChild(empty);
    } else {
        items.forEach(function (item) {
            const row = document.createElement('div');
            row.className = 'roulette-item-row';
            const label = document.createElement('div');
            label.className = 'roulette-item-label';
            label.textContent = item.label || '-';
            const probability = document.createElement('div');
            probability.className = 'roulette-item-probability';
            probability.textContent = formatProbability(item.probabilityBasisPoints);
            const reward = document.createElement('div');
            reward.className = 'roulette-item-reward';
            reward.textContent = item.losingItem ? '꽝' : rewardLabel(item);
            row.appendChild(label);
            row.appendChild(probability);
            row.appendChild(reward);
            itemList.appendChild(row);
        });
    }
    updateRouletteStatus(table);
}

function renderRouletteEvents(page) {
    const list = document.getElementById('roulette-event-list');
    const pagination = document.getElementById('roulette-event-pagination');
    if (!list) {
        return;
    }
    list.innerHTML = '';
    const events = Array.isArray(page.content) ? page.content : [];
    if (events.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'roulette-empty';
        empty.textContent = '룰렛 실행 기록이 없습니다.';
        list.appendChild(empty);
    } else {
        list.appendChild(createRouletteEventHeader());
        events.forEach(function (item) {
            list.appendChild(createRouletteEventRow(item));
        });
    }
    renderRouletteEventPagination(page, pagination);
}

function createRouletteEventHeader() {
    const row = document.createElement('div');
    row.className = 'roulette-event-row is-head';
    ['ID', '닉네임', '후원', '횟수', '상태', '재송출'].forEach(function (text) {
        const cell = document.createElement('span');
        cell.textContent = text;
        row.appendChild(cell);
    });
    return row;
}

function createRouletteEventRow(item) {
    const row = document.createElement('div');
    row.className = 'roulette-event-row';
    row.appendChild(rouletteEventCell('#' + (item.eventId || '-')));
    row.appendChild(rouletteEventCell(item.nickNameSnapshot || item.userId || '-'));
    row.appendChild(rouletteEventCell(formatNumber(item.donationAmount), 'numeric'));
    row.appendChild(rouletteEventCell(formatNumber(item.roundCount) + '회', 'numeric'));
    row.appendChild(rouletteEventCell(rouletteEventStatusLabel(item.status)));

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'roulette-event-replay';
    button.dataset.eventId = item.eventId;
    button.textContent = '재송출';
    row.appendChild(button);
    return row;
}

function rouletteEventCell(text, className) {
    const cell = document.createElement('span');
    if (className) {
        cell.className = className;
    }
    cell.textContent = text;
    return cell;
}

function renderRouletteEventPagination(page, pagination) {
    if (!pagination) {
        return;
    }
    pagination.innerHTML = '';
    const totalPages = page.totalPages || 0;
    if (totalPages <= 1) {
        return;
    }
    const current = page.number || 0;
    pagination.appendChild(createRouletteEventPageButton('이전', current - 1, current <= 0));
    for (let index = 0; index < totalPages; index++) {
        const button = createRouletteEventPageButton(String(index + 1), index, false);
        button.classList.toggle('is-active', index === current);
        pagination.appendChild(button);
    }
    pagination.appendChild(createRouletteEventPageButton('다음', current + 1, current >= totalPages - 1));
}

function createRouletteEventPageButton(label, page, disabled) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'roulette-event-page-button';
    button.dataset.page = page;
    button.disabled = disabled;
    button.textContent = label;
    return button;
}

function updateRouletteStatus(table) {
    const totalEl = document.getElementById('roulette-probability-total');
    const losingEl = document.getElementById('roulette-losing-state');
    const activeEl = document.getElementById('roulette-active-state');
    const reasonsEl = document.getElementById('roulette-reasons');
    const simulationEl = document.getElementById('roulette-simulation');
    if (simulationEl) {
        simulationEl.innerHTML = '';
    }
    if (!table) {
        if (totalEl) {
            totalEl.textContent = formatProbabilityTotal(0);
        }
        if (losingEl) {
            losingEl.textContent = '없음';
        }
        if (activeEl) {
            activeEl.textContent = '비활성';
        }
        if (reasonsEl) {
            reasonsEl.textContent = '';
        }
        return;
    }
    const validation = table.validation || {};
    if (totalEl) {
        totalEl.textContent = formatProbabilityTotal(validation.probabilityTotal || 0);
    }
    if (losingEl) {
        losingEl.textContent = validation.hasLosingItem ? '있음' : '없음';
    }
    if (activeEl) {
        activeEl.textContent = table.active ? '활성' : '비활성';
    }
    if (reasonsEl) {
        const reasons = Array.isArray(validation.reasons) ? validation.reasons : [];
        reasonsEl.innerHTML = '';
        if (reasons.length === 0) {
            const ok = document.createElement('span');
            ok.className = 'roulette-ok';
            ok.textContent = '활성화 가능';
            reasonsEl.appendChild(ok);
        } else {
            reasons.forEach(function (reason) {
                const chip = document.createElement('span');
                chip.textContent = rouletteReasonLabel(reason);
                reasonsEl.appendChild(chip);
            });
        }
    }
}

function createRouletteTable() {
    if (!requireAdminPermission()) {
        return;
    }
    const title = valueOf('roulette-title');
    const command = valueOf('roulette-command');
    const price = parseInt(valueOf('roulette-price'), 10);
    if (!title || !command || Number.isNaN(price) || price <= 0) {
        showToast('테이블 이름, 명령어, 1회 금액을 입력해 주세요.');
        return;
    }
    fetch(buildUrl('/admin/roulette/tables'), {
        method: 'POST',
        headers: csrfHeaders({'Content-Type': 'application/json'}),
        body: JSON.stringify({
            title: title,
            command: command,
            pricePerRound: price,
            highRoundThreshold: 100
        })
    })
        .then(requireOk)
        .then(function (table) {
            selectedRouletteTableId = table.id;
            clearRouletteTableForm();
            showToast('룰렛 생성 완료');
            loadRouletteTables();
        })
        .catch(function () {
            showToast('룰렛 생성에 실패했습니다.');
        });
}

function addRouletteItem() {
    if (!requireAdminPermission()) {
        return;
    }
    const table = getSelectedRouletteTable();
    if (!table) {
        showToast('룰렛 테이블을 선택해 주세요.');
        return;
    }
    const label = valueOf('roulette-item-label');
    const probability = parsePercentageToBasisPoints(valueOf('roulette-item-probability'));
    const losing = Boolean(document.getElementById('roulette-item-losing') &&
        document.getElementById('roulette-item-losing').checked);
    const rewardType = losing ? 'CUSTOM' : valueOf('roulette-item-reward');
    const conversionMode = losing ? 'NONE' : valueOf('roulette-item-conversion');
    const rawValue = valueOf('roulette-item-value');
    const exchangeFavoriteValue = rawValue ? parseInt(rawValue, 10) : null;
    if (!label || probability === null) {
        showToast('항목 이름과 확률(%)을 입력해 주세요.');
        return;
    }
    if (conversionMode === 'AUTO' && (!exchangeFavoriteValue || exchangeFavoriteValue === 0)) {
        showToast('자동 반영 항목은 호감도 값을 입력해 주세요.');
        return;
    }
    fetch(buildUrl('/admin/roulette/tables/' + table.id + '/items'), {
        method: 'POST',
        headers: csrfHeaders({'Content-Type': 'application/json'}),
        body: JSON.stringify({
            label: label,
            probabilityBasisPoints: probability,
            losingItem: losing,
            rewardType: rewardType,
            conversionMode: conversionMode,
            exchangeFavoriteValue: exchangeFavoriteValue,
            displayOrder: (table.items || []).length + 1
        })
    })
        .then(requireOk)
        .then(function () {
            clearRouletteItemForm();
            showToast('룰렛 항목 추가 완료');
            loadRouletteTables();
        })
        .catch(function () {
            showToast('룰렛 항목 추가에 실패했습니다.');
        });
}

function activateRouletteTable() {
    const table = getSelectedRouletteTable();
    if (!table) {
        return;
    }
    postRouletteAction('/admin/roulette/tables/' + table.id + '/activate', '룰렛 활성화 완료', '룰렛 활성화에 실패했습니다.');
}

function deactivateRouletteTable() {
    const table = getSelectedRouletteTable();
    if (!table) {
        return;
    }
    postRouletteAction('/admin/roulette/tables/' + table.id + '/deactivate', '룰렛 비활성화 완료', '룰렛 비활성화에 실패했습니다.');
}

function postRouletteAction(path, successMessage, failureMessage) {
    if (!requireAdminPermission()) {
        return;
    }
    fetch(buildUrl(path), {
        method: 'POST',
        headers: csrfHeaders()
    })
        .then(requireOk)
        .then(function (table) {
            selectedRouletteTableId = table.id;
            showToast(successMessage);
            loadRouletteTables();
        })
        .catch(function () {
            showToast(failureMessage);
        });
}

function simulateRouletteTable() {
    const table = getSelectedRouletteTable();
    if (!table) {
        return;
    }
    fetch(buildUrl('/admin/roulette/tables/' + table.id + '/simulation?iterations=10000'))
        .then(requireOk)
        .then(renderRouletteSimulation)
        .catch(function () {
            showToast('룰렛 시뮬레이션에 실패했습니다.');
        });
}

function issueOverlayToken() {
    if (!requireAdminPermission()) {
        return;
    }
    fetch(buildUrl('/admin/overlay/roulette/token'), {
        method: 'POST',
        headers: csrfHeaders()
    })
        .then(requireOk)
        .then(function (data) {
            const tokenUrl = window.location.origin + buildUrl('/overlay/roulette') + '#token=' + data.token;
            setValue('overlay-token-url', tokenUrl);
            showToast('오버레이 토큰 발급 완료');
        })
        .catch(function () {
            showToast('오버레이 토큰 발급에 실패했습니다.');
        });
}

function replayOverlayEvent() {
    if (!requireAdminPermission()) {
        return;
    }
    const rouletteEventId = parseInt(valueOf('overlay-replay-event-id'), 10);
    if (Number.isNaN(rouletteEventId) || rouletteEventId <= 0) {
        showToast('재송출 실행 ID를 입력해 주세요.');
        return;
    }
    replayOverlayEventById(rouletteEventId);
}

function replayOverlayEventById(rouletteEventId) {
    if (!requireAdminPermission()) {
        return;
    }
    if (!rouletteEventId || Number.isNaN(Number(rouletteEventId))) {
        showToast('재송출 실행 ID를 확인해 주세요.');
        return;
    }
    fetch(buildUrl('/admin/overlay/roulette/events/' + rouletteEventId + '/replay'), {
        method: 'POST',
        headers: csrfHeaders()
    })
        .then(requireOk)
        .then(function () {
            showToast('오버레이 재송출 대기열에 추가했습니다.');
        })
        .catch(function () {
            showToast('오버레이 재송출에 실패했습니다.');
        });
}

function renderRouletteSimulation(data) {
    const simulationEl = document.getElementById('roulette-simulation');
    if (!simulationEl) {
        return;
    }
    simulationEl.innerHTML = '';
    const items = Array.isArray(data.items) ? data.items : [];
    items.forEach(function (item) {
        const row = document.createElement('div');
        row.className = 'roulette-simulation-row';
        const label = document.createElement('span');
        label.textContent = item.label;
        const value = document.createElement('strong');
        value.textContent = item.count + '회 · ' + Math.round((item.ratio || 0) * 10000) / 100 + '%';
        row.appendChild(label);
        row.appendChild(value);
        simulationEl.appendChild(row);
    });
}

function clearRouletteTableForm() {
    setValue('roulette-title', '');
    setValue('roulette-command', '!룰렛');
    setValue('roulette-price', '1000');
}

function clearRouletteItemForm() {
    setValue('roulette-item-label', '');
    setValue('roulette-item-probability', '');
    setValue('roulette-item-value', '');
    const losing = document.getElementById('roulette-item-losing');
    if (losing) {
        losing.checked = false;
    }
}

function rewardLabel(item) {
    const value = item.exchangeFavoriteValue;
    if (value !== null && value !== undefined) {
        return item.rewardType + ' ' + (value > 0 ? '+' + value : value);
    }
    return item.rewardType + ' · ' + item.conversionMode;
}

function valueOf(id) {
    const element = document.getElementById(id);
    return element ? element.value.trim() : '';
}

function setValue(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.value = value;
    }
}

function activateAdminTab(targetId) {
    document.querySelectorAll('.admin-tab').forEach(function (button) {
        const active = button.getAttribute('data-tab-target') === targetId;
        button.classList.toggle('is-active', active);
        button.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    document.querySelectorAll('.tab-panel').forEach(function (panel) {
        panel.classList.toggle('is-active', panel.id === targetId);
    });

    if (targetId === 'attendance-tab') {
        startAttendanceManagement();
    } else {
        stopAttendanceManagement();
    }

    if (targetId === 'roulette-tab') {
        loadRouletteManagement();
    }
}

function requireOk(response) {
    if (!response.ok) {
        throw new Error('REQUEST_FAILED');
    }
    return response.json();
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString('ko-KR');
}

function formatProbability(value) {
    const basisPoints = Number(value || 0);
    return formatPercentValue(basisPoints / 100) + '%';
}

function formatProbabilityTotal(value) {
    const basisPoints = Number(value || 0);
    return formatProbability(basisPoints) + ' / 100%';
}

function rouletteReasonLabel(reason) {
    const labels = {
        'command is required': '명령어를 입력해야 합니다.',
        'pricePerRound is required': '1회 금액을 입력해야 합니다.',
        'probability total must be 10000': '확률 합계가 100%가 되어야 합니다.',
        'losing item is required': '꽝 항목이 하나 이상 필요합니다.'
    };
    return labels[reason] || reason;
}

function rouletteEventStatusLabel(status) {
    const labels = {
        CONFIRMED: '확정',
        APPLIED: '반영',
        PARTIALLY_APPLIED: '부분 반영',
        FAILED: '실패'
    };
    return labels[status] || status || '-';
}

function parsePercentageToBasisPoints(value) {
    if (!value) {
        return null;
    }
    const percent = Number(value);
    if (!Number.isFinite(percent) || percent < 0 || percent > 100) {
        return null;
    }
    return Math.round(percent * 100);
}

function formatPercentValue(value) {
    return Number(value || 0).toLocaleString('ko-KR', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });
}

function loadAdjustments() {
    if (!getUiPermissions().isAdmin) {
        showToast('권한이 없습니다.');
        return Promise.reject(new Error('FORBIDDEN'));
    }
    if (adjustmentsCache) {
        renderAdjustments(adjustmentsCache);
        return Promise.resolve();
    }

    return fetch(buildUrl('/favorite/adjustments'))
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Failed to load adjustments');
            }
            return response.json();
        })
        .then(function (data) {
            adjustmentsCache = data;
            renderAdjustments(data);
        })
        .catch(function () {
            showToast('권한이 없습니다.');
        });
}

function renderAdjustments(items) {
    const list = document.getElementById('adjustment-list');
    list.innerHTML = '';
    items.forEach(function (item) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'adjustment-button';
        const amountValue = typeof item.amount === 'number' ? item.amount : parseInt(item.amount, 10) || 0;
        if (amountValue < 0) {
            button.classList.add('is-negative');
        } else if (amountValue > 0) {
            button.classList.add('is-positive');
        }
        if (selectedIds.has(item.id)) {
            button.classList.add('is-selected');
        }
        const label = document.createElement('span');
        label.textContent = item.label;
        const amount = document.createElement('strong');
        amount.textContent = amountValue > 0 ? '+' + amountValue : String(amountValue);
        button.appendChild(label);
        button.appendChild(amount);
        button.addEventListener('click', function (event) {
            event.preventDefault();
            toggleSelection(item.id, button);
        });
        list.appendChild(button);
    });
    updateCalc();
}

function toggleSelection(id, button) {
    if (selectedIds.has(id)) {
        selectedIds.delete(id);
        button.classList.remove('is-selected');
    } else {
        selectedIds.add(id);
        button.classList.add('is-selected');
    }
    updateCalc();
}

function getManualAmount() {
    const manualAmount = document.getElementById('manual-amount');
    if (!manualAmount) {
        return 0;
    }
    const value = parseInt(manualAmount.value.trim(), 10);
    return Number.isNaN(value) ? 0 : value;
}

function updateCalc() {
    if (!adjustmentsCache) {
        return;
    }
    let delta = 0;
    adjustmentsCache.forEach(function (item) {
        if (selectedIds.has(item.id)) {
            delta += item.amount;
        }
    });
    const manualAmount = getManualAmount();
    const calcManual = document.getElementById('calc-manual');
    if (calcManual) {
        calcManual.textContent = manualAmount;
    }
    delta += manualAmount;
    document.getElementById('calc-delta').textContent = delta;
    document.getElementById('calc-after').textContent = currentFavorite + delta;
}

function applyAdjustments() {
    if (!requireAdminPermission()) {
        return;
    }
    if (!currentUserId) {
        return;
    }
    const ids = Array.from(selectedIds);
    const manualAmount = getManualAmount();
    const manualHistory = (document.getElementById('manual-history') || {}).value || '';

    if (ids.length === 0 && manualAmount === 0) {
        showToast('업보를 선택하거나 수동 수치를 입력해 주세요.');
        return;
    }

    fetch(buildUrl('/favorite/adjustments/apply'), {
        method: 'POST',
        headers: csrfHeaders({'Content-Type': 'application/json'}),
        body: JSON.stringify({
            userId: currentUserId,
            adjustmentIds: ids,
            manualAmount: manualAmount === 0 ? null : manualAmount,
            manualHistory: manualHistory.trim()
        })
    })
        .then(function (response) {
            if (response.status === 403) {
                throw new Error('FORBIDDEN');
            }
            if (!response.ok) {
                throw new Error('Failed to apply adjustments');
            }
            return response.json();
        })
        .then(function (data) {
            const row = document.querySelector('.board-row[data-user-id="' + data.userId + '"]');
            if (row) {
                row.setAttribute('data-favorite', data.afterFavorite);
                const score = row.querySelector('.score');
                if (score) {
                    score.textContent = data.afterFavorite;
                }
            }
            showToast('업보 적용 완료');
            closeModal();
            setTimeout(function () {
                window.location.reload();
            }, 400);
        })
        .catch(function (error) {
            if (error.message === 'FORBIDDEN') {
                showToast('권한이 없습니다.');
                return;
            }
            showToast('업보 적용에 실패했습니다.');
        });
}

document.addEventListener('DOMContentLoaded', function () {
    let syncButton = document.getElementById('sync-button');
    if (syncButton) {
        syncButton.addEventListener('click', function (event) {
            event.preventDefault();
            if (!requireAdminPermission()) {
                return;
            }

            let spinner = document.getElementById('loading-spinner');
            if (spinner) {
                spinner.style.display = 'flex';
            }

            $.get(buildUrl('/google/sync'))
                .done(function (response) {
                    if (response === 'SUCCESS') {
                        showToast('데이터 동기화 완료!');
                        setTimeout(function () {
                            window.location.reload();
                        }, 400);
                    } else {
                        showToast('데이터 동기화 실패!');
                    }
                })
                .fail(function () {
                    showToast('권한이 없습니다.');
                })
                .always(function () {
                    if (spinner) {
                        spinner.style.display = 'none';
                    }
                });
        });
    }

    document.querySelectorAll('.admin-tab').forEach(function (button) {
        button.addEventListener('click', function (event) {
            event.preventDefault();
            activateAdminTab(button.getAttribute('data-tab-target'));
        });
    });

    document.querySelectorAll('.board-row').forEach(function (row) {
        row.addEventListener('click', function (event) {
            if (event.target.closest('.karma-button')) {
                return;
            }
            const rowUserId = row.getAttribute('data-user-id');
            if (!rowUserId || !canViewHistory(rowUserId)) {
                showToast('권한이 없습니다.');
                return;
            }
            const details = row.querySelector('.history-details');
            if (!details) {
                return;
            }
            if (details.style.display === 'block') {
                details.style.display = 'none';
                return;
            }
            details.style.display = 'block';
            loadHistoryForRow(row);
        });
    });

    document.addEventListener('click', function (event) {
        const karmaButton = event.target.closest('.karma-button');
        if (karmaButton) {
            event.preventDefault();
            event.stopPropagation();
            const row = karmaButton.closest('.board-row');
            if (row) {
                openModal(row);
            }
            return;
        }

        if (event.target.id === 'karma-close') {
            event.preventDefault();
            closeModal();
            return;
        }

        if (event.target.id === 'attendance-apply') {
            event.preventDefault();
            applyAttendance();
            return;
        }

        if (event.target.id === 'roulette-create') {
            event.preventDefault();
            createRouletteTable();
            return;
        }

        if (event.target.id === 'roulette-item-add') {
            event.preventDefault();
            addRouletteItem();
            return;
        }

        if (event.target.id === 'roulette-activate') {
            event.preventDefault();
            activateRouletteTable();
            return;
        }

        if (event.target.id === 'roulette-deactivate') {
            event.preventDefault();
            deactivateRouletteTable();
            return;
        }

        if (event.target.id === 'roulette-simulate') {
            event.preventDefault();
            simulateRouletteTable();
            return;
        }

        if (event.target.id === 'overlay-token-issue') {
            event.preventDefault();
            issueOverlayToken();
            return;
        }

        const rouletteEventReplay = event.target.closest('.roulette-event-replay');
        if (rouletteEventReplay) {
            event.preventDefault();
            replayOverlayEventById(rouletteEventReplay.dataset.eventId);
            return;
        }

        const rouletteEventPageButton = event.target.closest('.roulette-event-page-button');
        if (rouletteEventPageButton) {
            event.preventDefault();
            loadRouletteEvents(parseInt(rouletteEventPageButton.dataset.page, 10));
            return;
        }

        if (event.target.id === 'overlay-replay') {
            event.preventDefault();
            replayOverlayEvent();
            return;
        }

        if (event.target.id === 'karma-clear') {
            event.preventDefault();
            selectedIds.clear();
            document.querySelectorAll('.adjustment-button.is-selected').forEach(function (button) {
                button.classList.remove('is-selected');
            });
            const manualAmount = document.getElementById('manual-amount');
            const manualHistory = document.getElementById('manual-history');
            if (manualAmount) {
                manualAmount.value = '';
            }
            if (manualHistory) {
                manualHistory.value = '';
            }
            updateCalc();
            return;
        }

        if (event.target.id === 'karma-apply') {
            event.preventDefault();
            applyAdjustments();
            return;
        }

        if (event.target.id === 'karma-modal') {
            closeModal();
            return;
        }

    });

    const manualAmount = document.getElementById('manual-amount');
    const manualHistory = document.getElementById('manual-history');
    if (manualAmount) {
        manualAmount.addEventListener('input', updateCalc);
    }
    if (manualHistory) {
        manualHistory.addEventListener('input', updateCalc);
    }
});
