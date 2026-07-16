(function () {
    "use strict";

    document.addEventListener("click", function (event) {
        const variableButton = event.target.closest("[data-template-variable][data-template-target]");
        if (!variableButton) {
            return;
        }

        const textarea = document.querySelector(variableButton.dataset.templateTarget);
        if (!textarea) {
            return;
        }

        const token = variableButton.dataset.templateVariable;
        const start = textarea.selectionStart ?? textarea.value.length;
        const end = textarea.selectionEnd ?? start;
        textarea.setRangeText(token, start, end, "end");
        textarea.dispatchEvent(new Event("input", {bubbles: true}));
        textarea.focus();

        const dropdown = variableButton.closest(".dropdown");
        const toggle = dropdown?.querySelector("[data-bs-toggle='dropdown']");
        if (toggle && window.bootstrap?.Dropdown) {
            window.bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
        }
    });

    function clearStaleReview(event) {
        const form = event.target.closest("form[data-review-target]");
        if (!form || event.target.type === "hidden") {
            return;
        }

        const review = document.querySelector(form.dataset.reviewTarget);
        if (review) {
            review.replaceChildren();
        }
    }

    document.addEventListener("input", clearStaleReview);
    document.addEventListener("change", clearStaleReview);

    document.addEventListener("htmx:afterSwap", function (event) {
        const target = event.detail.target;
        if (!target?.matches("#command-editor-region, #timer-editor-region")) {
            return;
        }

        target.querySelector("[data-editor-heading]")?.focus({preventScroll: true});
    });
}());
