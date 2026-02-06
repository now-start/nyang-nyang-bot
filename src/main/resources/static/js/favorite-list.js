function showToast(text) {
    let toast = document.getElementById('toast');
    let toastNickname = document.getElementById('toast-text');
    toastNickname.textContent = text;
    toast.style.display = 'block';
    setTimeout(function () {
        toast.style.display = 'none';
    }, 2000);
}

let adjustmentsCache = null;
let selectedIds = new Set();
let currentUserId = null;
let currentFavorite = 0;

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
    const adjustmentLabel = document.getElementById('adjustment-label');
    const adjustmentAmount = document.getElementById('adjustment-amount');
    if (adjustmentLabel) {
        adjustmentLabel.value = '';
    }
    if (adjustmentAmount) {
        adjustmentAmount.value = '';
    }
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
        if (selectedIds.has(item.id)) {
            button.classList.add('is-selected');
        }
        const label = document.createElement('span');
        label.textContent = item.label;
        const amount = document.createElement('strong');
        amount.textContent = '+' + item.amount;
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

function createAdjustment() {
    const label = document.getElementById('adjustment-label').value.trim();
    const amountRaw = document.getElementById('adjustment-amount').value.trim();
    const amount = parseInt(amountRaw, 10);

    if (!label || Number.isNaN(amount)) {
        showToast('항목명과 수치를 입력해 주세요.');
        return;
    }

    fetch('/favorite/adjustments', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({label: label, amount: amount})
    })
        .then(function (response) {
            if (response.status === 403) {
                throw new Error('FORBIDDEN');
            }
            if (!response.ok) {
                throw new Error('Failed to create adjustment');
            }
            return response.json();
        })
        .then(function () {
            showToast('항목이 추가되었습니다.');
            document.getElementById('adjustment-label').value = '';
            document.getElementById('adjustment-amount').value = '';
            adjustmentsCache = null;
            loadAdjustments();
        })
        .catch(function (error) {
            if (error.message === 'FORBIDDEN') {
                showToast('권한이 없습니다.');
                return;
            }
            showToast('항목 추가에 실패했습니다.');
        });
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
                    showToast('동기화 중 오류가 발생했습니다.');
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

        if (event.target.id === 'adjustment-add') {
            event.preventDefault();
            createAdjustment();
            return;
        }

        if (event.target.id === 'karma-modal') {
            closeModal();
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
