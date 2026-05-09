(function () {
    const root = document.getElementById('overlay-root');
    const donorEl = document.getElementById('roulette-donor');
    const roundEl = document.getElementById('roulette-round');
    const resultEl = document.getElementById('roulette-result');
    const subEl = document.getElementById('roulette-sub');
    const summaryEl = document.getElementById('roulette-summary');
    const token = readToken();
    let displaying = false;

    if (!token) {
        return;
    }

    poll();
    window.setInterval(poll, 2000);

    function readToken() {
        const hash = window.location.hash ? window.location.hash.slice(1) : '';
        const params = new URLSearchParams(hash);
        return params.get('token');
    }

    function poll() {
        if (displaying) {
            return;
        }
        fetch('/overlay/roulette/events/next', {
            headers: {'Authorization': 'Bearer ' + token}
        })
            .then(function (response) {
                if (response.status === 204) {
                    return null;
                }
                if (!response.ok) {
                    throw new Error('overlay request failed');
                }
                return response.json();
            })
            .then(function (event) {
                if (event) {
                    return displayEvent(event);
                }
                return null;
            })
            .catch(function () {
                hide();
            });
    }

    async function displayEvent(event) {
        displaying = true;
        const rounds = Array.isArray(event.rounds) ? event.rounds : [];
        const maxAnimatedRounds = Math.max(0, event.maxAnimatedRounds || 0);
        const animatedRounds = rounds.slice(0, maxAnimatedRounds);
        const summaryRounds = rounds.slice(maxAnimatedRounds);

        for (let index = 0; index < animatedRounds.length; index++) {
            await displayRound(event, animatedRounds[index], index + 1, rounds.length);
        }
        if (summaryRounds.length > 0) {
            await displaySummary(event, summaryRounds);
        }
        await postDisplayed(event.displayEventId);
        hide();
        displaying = false;
    }

    function displayRound(event, round, roundNo, totalRounds) {
        return new Promise(function (resolve) {
            const labels = (event.rounds || []).map(function (item) {
                return item.itemLabel;
            }).filter(Boolean);
            let cursor = 0;
            show();
            donorEl.textContent = event.nickName || '후원자';
            roundEl.textContent = roundNo + ' / ' + totalRounds;
            subEl.textContent = '룰렛 진행 중';
            summaryEl.innerHTML = '';
            resultEl.className = 'roulette-result';
            const ticker = window.setInterval(function () {
                resultEl.textContent = labels.length > 0 ? labels[cursor % labels.length] : '...';
                cursor++;
            }, 80);

            window.setTimeout(function () {
                window.clearInterval(ticker);
                resultEl.textContent = round.itemLabel || '-';
                resultEl.className = 'roulette-result ' + resultClass(round);
                subEl.textContent = round.losingItem ? '다음 기회에' : resultSubText(round);
                window.setTimeout(resolve, 1500);
            }, 2600);
        });
    }

    function displaySummary(event, rounds) {
        return new Promise(function (resolve) {
            show();
            donorEl.textContent = event.nickName || '후원자';
            roundEl.textContent = '요약';
            resultEl.className = 'roulette-result';
            resultEl.textContent = '추가 ' + rounds.length + '회 결과';
            subEl.textContent = '';
            summaryEl.innerHTML = '';
            const counts = new Map();
            rounds.forEach(function (round) {
                const label = round.itemLabel || '-';
                counts.set(label, (counts.get(label) || 0) + 1);
            });
            counts.forEach(function (count, label) {
                const row = document.createElement('div');
                row.className = 'roulette-summary-row';
                const labelEl = document.createElement('span');
                labelEl.textContent = label;
                const countEl = document.createElement('strong');
                countEl.textContent = 'x' + count;
                row.appendChild(labelEl);
                row.appendChild(countEl);
                summaryEl.appendChild(row);
            });
            window.setTimeout(resolve, 3200);
        });
    }

    function postDisplayed(displayEventId) {
        if (!displayEventId) {
            return Promise.resolve();
        }
        return fetch('/overlay/roulette/events/' + displayEventId + '/displayed', {
            method: 'POST',
            headers: {'Authorization': 'Bearer ' + token}
        }).catch(function () {
            return null;
        });
    }

    function resultClass(round) {
        if (round.losingItem) {
            return 'is-losing';
        }
        if (round.rewardType === 'FAVORITE') {
            return 'is-positive';
        }
        return 'is-warning';
    }

    function resultSubText(round) {
        if (round.exchangeFavoriteValue !== null && round.exchangeFavoriteValue !== undefined) {
            const value = Number(round.exchangeFavoriteValue);
            return '호감도 ' + (value > 0 ? '+' + value : value);
        }
        return '마이페이지에서 확인';
    }

    function show() {
        root.classList.add('is-visible');
    }

    function hide() {
        root.classList.remove('is-visible');
        resultEl.textContent = '';
        subEl.textContent = '';
        summaryEl.innerHTML = '';
    }
})();
