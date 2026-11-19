(function (window, document) {
    "use strict";

    if (window._upstreamDialogValidationBound) {
        return;
    }
    window._upstreamDialogValidationBound = true;

    var DIALOG_CLASS = "upstream-dialog";
    var TRACKING_MARKER = "dialogs:upstream";
    var POPUP_ID = "upstream-validation-dialog";
    var SKIP_NAMES = {
        "./sling:resourceType": true,
        "./jcr:primaryType": true,
        "./jcr:lastModified": true,
        "./jcr:lastModifiedBy": true,
        "./textIsRich": true
    };

    function get$() {
        return (window.Granite && window.Granite.$) || window.jQuery;
    }

    function isSkipName(name) {
        if (!name || name.indexOf("./") !== 0) {
            return true;
        }
        if (name.indexOf("@") !== -1) {
            return true;
        }
        if (SKIP_NAMES[name]) {
            return true;
        }
        if (name.indexOf("./jcr:") === 0) {
            return true;
        }
        if (/IsRich$/i.test(name)) {
            return true;
        }
        return false;
    }

    function closestDialog(el) {
        if (!el) {
            return null;
        }
        if (el.closest) {
            return el.closest("coral-dialog, .cq-Dialog, form.cq-dialog");
        }
        var $ = get$();
        return $ ? $(el).closest("coral-dialog, .cq-Dialog, form.cq-dialog")[0] : null;
    }

    function isTargetDialog(form) {
        if (!form) {
            return false;
        }
        var $ = get$();
        var $form = $ ? $(form) : null;
        var dialog = closestDialog(form) || form;

        if (form.classList && form.classList.contains(DIALOG_CLASS)) {
            return true;
        }
        if (dialog.classList && dialog.classList.contains(DIALOG_CLASS)) {
            return true;
        }
        if ($form && ($form.hasClass(DIALOG_CLASS) || $form.find("." + DIALOG_CLASS).length)) {
            return true;
        }
        if ($ && ($(dialog).hasClass(DIALOG_CLASS) || $(dialog).find("." + DIALOG_CLASS).length)) {
            return true;
        }

        var tracking = "";
        if (dialog.getAttribute) {
            tracking = dialog.getAttribute("trackingFeature")
                || dialog.getAttribute("trackingfeature")
                || "";
        }
        if (!tracking && dialog.trackingFeature) {
            tracking = dialog.trackingFeature;
        }
        if (tracking.indexOf(TRACKING_MARKER) !== -1) {
            return true;
        }
        return false;
    }

    function collectFields(form) {
        var $ = get$();
        var fields = [];
        if (!$) {
            return fields;
        }

        $(form).find(".coral-Form-fieldwrapper").each(function () {
            var $wrapper = $(this);
            var label = $wrapper.find("label, .coral-Form-fieldlabel").first().text() || "";
            label = String(label).replace(/\s*\*\s*$/, "").replace(/\s+/g, " ").trim();

            var el = null;
            $wrapper.find("[name^='./']").each(function () {
                if (!isSkipName(this.getAttribute("name"))) {
                    el = this;
                    return false;
                }
            });
            if (!el) {
                el = $wrapper.find("foundation-autocomplete, .cq-RichText, .coral-Form-field, coral-select, coral-datepicker, coral-numberfield, coral-checkbox, coral-fileupload, coral-multifield").get(0);
            }
            if (!el) {
                return;
            }
            if (!label) {
                label = String(el.getAttribute("name") || el.getAttribute("aria-label") || "Field").replace(/^\.\//, "");
            }
            fields.push({
                el: el,
                label: label,
                wrapper: this
            });
        });
        return fields;
    }

    function getRteValue($wrapper, el) {
        var $container = $wrapper && $wrapper.length
            ? $wrapper.find(".cq-RichText, .richtext-container, .coral-RichText")
            : $(el).closest(".cq-RichText, .richtext-container, .coral-RichText");
        if (!$container.length) {
            return null;
        }
        var $editable = $container.find(".cq-RichText-editable, .coral-RichText-editable, [contenteditable='true']").first();
        if ($editable.length) {
            return $editable.html() || $editable.text() || "";
        }
        return $container.find("input[data-cq-richtext-input], input[name], textarea[name]").val() || "";
    }

    function getFoundationValue(el) {
        var $ = get$();
        try {
            var $el = $(el);
            if ($el.adaptTo) {
                var api = $el.adaptTo("foundation-field");
                if (api && typeof api.getValue === "function") {
                    return api.getValue();
                }
            }
        } catch (ignore) {
            // Field has no foundation adapter.
        }
        return null;
    }

    function getFieldValue(field) {
        var $ = get$();
        var el = field.el;
        var $wrapper = field.wrapper ? $(field.wrapper) : $(el).closest(".coral-Form-fieldwrapper");

        var rteValue = getRteValue($wrapper, el);
        if (rteValue !== null) {
            return rteValue;
        }

        var foundationValue = getFoundationValue(el);
        if (foundationValue !== null && typeof foundationValue !== "undefined") {
            return foundationValue;
        }

        if (typeof el.value !== "undefined" && el.value !== null && el.value !== "") {
            return el.value;
        }
        return $(el).val();
    }

    function isEmptyValue(value) {
        if (value === null || typeof value === "undefined") {
            return true;
        }
        if (Object.prototype.toString.call(value) === "[object Array]") {
            return value.length === 0 || value.every(isEmptyValue);
        }
        var text = String(value)
            .replace(/<[^>]*>/g, " ")
            .replace(/&nbsp;/gi, " ")
            .replace(/\s+/g, " ")
            .trim();
        return text.length === 0;
    }

    function buildMessage(missingLabels) {
        if (!missingLabels.length) {
            return null;
        }
        if (missingLabels.length === 1) {
            return missingLabels[0] + " is not authored in the cq:dialog.";
        }
        if (missingLabels.length === 2) {
            return missingLabels[0] + " and " + missingLabels[1] + " are not authored in the cq:dialog.";
        }
        var labels = missingLabels.slice();
        var last = labels.pop();
        return labels.join(", ") + ", and " + last + " are not authored in the cq:dialog.";
    }

    function validateForm(form) {
        var fields = collectFields(form);
        if (!fields.length) {
            return null;
        }
        var missingLabels = [];
        var i;
        for (i = 0; i < fields.length; i++) {
            if (isEmptyValue(getFieldValue(fields[i]))) {
                missingLabels.push(fields[i].label);
            }
        }
        if (!missingLabels.length) {
            return null;
        }
        if (missingLabels.length === fields.length) {
            return "None of the fields are authored in the cq:dialog.";
        }
        return buildMessage(missingLabels);
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function showInlineAlert(form, message) {
        var $ = get$();
        if (!$) {
            return;
        }
        var $form = $(form);
        $form.find(".upstream-validation-alert").remove();
        var html = '<coral-alert class="upstream-validation-alert" variant="error" style="margin:0.75rem 1rem 0;">'
            + "<coral-alert-header>Validation Required</coral-alert-header>"
            + "<coral-alert-content>" + escapeHtml(message) + "</coral-alert-content>"
            + "</coral-alert>";
        var $content = $form.find("coral-dialog-content").first();
        if ($content.length) {
            $content.prepend(html);
        } else {
            $form.prepend(html);
        }
    }

    function showPopup(form, message) {
        showInlineAlert(form, message);

        var existing = document.getElementById(POPUP_ID);
        if (existing && existing.parentNode) {
            existing.parentNode.removeChild(existing);
        }

        function reveal(dialog) {
            dialog.style.zIndex = "20000";
            if (typeof dialog.show === "function") {
                dialog.show();
            } else {
                dialog.open = true;
                dialog.setAttribute("open", "");
            }
            window.setTimeout(function () {
                dialog.style.zIndex = "20000";
            }, 0);
        }

        if (window.Coral) {
            var dialog = document.createElement("coral-dialog");
            dialog.id = POPUP_ID;
            dialog.setAttribute("variant", "error");
            dialog.setAttribute("closable", "on");
            dialog.setAttribute("backdrop", "modal");
            dialog.innerHTML = "<coral-dialog-header>Validation Required</coral-dialog-header>"
                + "<coral-dialog-content><p>" + escapeHtml(message) + "</p></coral-dialog-content>"
                + '<coral-dialog-footer><button is="coral-button" variant="primary" coral-close>OK</button></coral-dialog-footer>';
            dialog.style.zIndex = "20000";
            document.body.appendChild(dialog);
            if (window.Coral.commons && typeof window.Coral.commons.ready === "function") {
                window.Coral.commons.ready(dialog, function () {
                    reveal(dialog);
                });
            } else {
                reveal(dialog);
            }
            return;
        }

        window.alert(message);
    }

    function rejectSubmit($) {
        var deferred = $.Deferred();
        deferred.reject();
        return {
            preResult: deferred.promise()
        };
    }

    function registerFoundationHook($) {
        var registry;
        try {
            registry = $(window).adaptTo("foundation-registry");
        } catch (ignore) {
            registry = null;
        }
        if (!registry || typeof registry.register !== "function") {
            return false;
        }
        registry.register("foundation.form.submit", {
            selector: "form.cq-dialog, form.foundation-form",
            handler: function (formEl) {
                if (!isTargetDialog(formEl)) {
                    return {};
                }
                var message = validateForm(formEl);
                if (message) {
                    showPopup(formEl, message);
                    return rejectSubmit($);
                }
                return {};
            }
        });
        return true;
    }

    function pathHasSubmit(path) {
        var i;
        var node;
        for (i = 0; i < path.length; i++) {
            node = path[i];
            if (!node || !node.classList) {
                continue;
            }
            if (node.classList.contains("cq-dialog-submit")) {
                return node;
            }
        }
        return null;
    }

    function getFormFromPath(path) {
        var i;
        var node;
        for (i = 0; i < path.length; i++) {
            node = path[i];
            if (!node) {
                continue;
            }
            if (node.tagName && node.tagName.toLowerCase() === "form"
                    && node.classList
                    && (node.classList.contains("cq-dialog") || node.classList.contains("foundation-form"))) {
                return node;
            }
        }
        return null;
    }

    function onDoneClick(e) {
        var path = e.composedPath ? e.composedPath() : (e.path || [e.target]);
        if (!pathHasSubmit(path)) {
            return;
        }
        var form = getFormFromPath(path);
        if (!form && e.target && e.target.closest) {
            form = e.target.closest("form.cq-dialog, form.foundation-form");
        }
        if (!form || !isTargetDialog(form)) {
            return;
        }
        var message = validateForm(form);
        if (!message) {
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        if (typeof e.stopImmediatePropagation === "function") {
            e.stopImmediatePropagation();
        }
        showPopup(form, message);
    }

    function init() {
        var $ = get$();
        if (!$) {
            window.setTimeout(init, 50);
            return;
        }

        registerFoundationHook($);
        document.addEventListener("click", onDoneClick, true);

        $(document).on("click", ".cq-dialog-submit", function (e) {
            var form = $(this).closest("form.cq-dialog, form.foundation-form")[0];
            if (!form || !isTargetDialog(form)) {
                return;
            }
            var message = validateForm(form);
            if (!message) {
                return;
            }
            e.preventDefault();
            e.stopImmediatePropagation();
            showPopup(form, message);
            return false;
        });
    }

    init();
})(window, document);
