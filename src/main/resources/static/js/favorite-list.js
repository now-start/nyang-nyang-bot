function showToast(text) {
    let toast = document.getElementById('toast');
    let toastNickname = document.getElementById('toast-text');
    toastNickname.textContent = text;
    toast.style.display = 'block';
    setTimeout(function () {
        toast.style.display = 'none';
    }, 5000);
}

let adjustmentsCache = null;
let selectedIds = new Set();
let currentUserId = null;
let currentFavorite = 0;
let attendanceUsers = [];
let attendanceSelectedIds = new Set();
let attendancePollingId = null;

function openModal(row) {
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

function openAttendanceModal() {
    attendanceSelectedIds.clear();
    const amountInput = document.getElementById('attendance-amount');
    if (amountInput) {
        amountInput.value = '1';
    }
    document.getElementById('attendance-modal').style.display = 'flex';
    startAttendanceCapture()
        .then(function () {
            fetchAttendanceUsers();
            startAttendancePolling();
        })
        .catch(function () {
            showToast('권한이 없습니다.');
        });
}

function closeAttendanceModal() {
    document.getElementById('attendance-modal').style.display = 'none';
    attendanceSelectedIds.clear();
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
    return fetch('/attendance/start', {method: 'POST'}).then(function (response) {
        if (!response.ok) {
            throw new Error('Failed to start attendance');
        }
    });
}

function stopAttendanceCapture() {
    return fetch('/attendance/stop', {method: 'POST'});
}

function fetchAttendanceUsers() {
    return fetch('/attendance/users')
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Failed to load attendance users');
            }
            return response.json();
        })
        .then(function (data) {
            attendanceUsers = Array.isArray(data) ? data : [];
            attendanceSelectedIds = new Set(attendanceUsers.map(function (user) {
                return user.userId;
            }));
            const availableIds = new Set(attendanceUsers.map(function (user) {
                return user.userId;
            }));
            attendanceSelectedIds.forEach(function (userId) {
                if (!availableIds.has(userId)) {
                    attendanceSelectedIds.delete(userId);
                }
            });
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
        button.classList.remove('is-selected');
    } else {
        attendanceSelectedIds.add(userId);
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

    fetch('/attendance/apply', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
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
            closeAttendanceModal();
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

function loadAdjustments() {
    if (adjustmentsCache) {
        renderAdjustments(adjustmentsCache);
        return Promise.resolve();
    }

    return fetch('/favorite/adjustments')
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
            showToast('업보 목록을 불러오지 못했습니다.');
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

    fetch('/favorite/adjustments/apply', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
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

            let spinner = document.getElementById('loading-spinner');
            if (spinner) {
                spinner.style.display = 'flex';
            }

            $.get('/google/sync')
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
    } else {
        console.error('Element with ID "sync-button" not found.');
    }

    const attendanceButton = document.getElementById('attendance-button');
    if (attendanceButton) {
        attendanceButton.addEventListener('click', function (event) {
            event.preventDefault();
            openAttendanceModal();
        });
    }

    document.querySelectorAll('.board-row').forEach(function (row) {
        row.addEventListener('click', function (event) {
            if (event.target.closest('.karma-button')) {
                return;
            }
            let historyDetails = this.querySelectorAll('.history-details');
            for (let i = 0; i < historyDetails.length; i++) {
                if (historyDetails[i].style.display === 'block') {
                    historyDetails[i].style.display = 'none';
                } else {
                    historyDetails[i].style.display = 'block';
                }
            }
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

        if (event.target.id === 'attendance-close' || event.target.id === 'attendance-cancel') {
            event.preventDefault();
            closeAttendanceModal();
            return;
        }

        if (event.target.id === 'attendance-apply') {
            event.preventDefault();
            applyAttendance();
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

        if (event.target.id === 'attendance-modal') {
            closeAttendanceModal();
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
