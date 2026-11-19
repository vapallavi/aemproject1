(function (document, $) {
    "use strict";

    var CHECKBOX_NAME = "./enablePii";
    var CSS_NAME = "./cssClass";
    var DELETE_NAME = "./cssClass@Delete";

    function findDialog(root) {
        var $root = $(root);
        return $root.closest("form.cq-dialog, .cq-dialog, coral-dialog").add(
            $root.find("form.cq-dialog, .cq-dialog")
        ).first();
    }

    function getCheckbox($scope) {
        return $scope.find("coral-checkbox[name='" + CHECKBOX_NAME + "']").add(
            $scope.find("input[name='" + CHECKBOX_NAME + "'][type='checkbox']")
        ).first();
    }

    function isChecked($scope) {
        var $checkbox = getCheckbox($scope);
        if (!$checkbox.length) {
            return false;
        }
        var el = $checkbox[0];
        if (typeof el.checked === "boolean") {
            return el.checked;
        }
        var api = $checkbox.adaptTo("foundation-field");
        if (api && typeof api.getValue === "function") {
            var value = api.getValue();
            return value === true || value === "true"
                || (Array.isArray(value) && value.indexOf("true") !== -1);
        }
        return $checkbox.is(":checked");
    }

    function getCssInput($scope) {
        return $scope.find("input[name='" + CSS_NAME + "']").first();
    }

    function getCssFieldApi($scope) {
        var $input = getCssInput($scope);
        var api = $input.adaptTo("foundation-field");
        if (api) {
            return api;
        }
        return $scope.find(".singlearticle-css-class").adaptTo("foundation-field");
    }

    function setCssEnabled($scope, enabled) {
        var api = getCssFieldApi($scope);
        var $input = getCssInput($scope);
        if (api && typeof api.setDisabled === "function") {
            api.setDisabled(!enabled);
        }
        if ($input.length) {
            $input.prop("disabled", !enabled);
            $input.attr("aria-disabled", enabled ? "false" : "true");
        }
    }

    function clearCssValue($scope) {
        var api = getCssFieldApi($scope);
        var $input = getCssInput($scope);
        if (api && typeof api.setValue === "function") {
            api.setValue("");
        }
        if ($input.length) {
            $input.val("");
            $input.attr("value", "");
        }
    }

    function ensureDeleteInput($form) {
        if (!$form.find("input[name='" + DELETE_NAME + "']").length) {
            $("<input>", {
                type: "hidden",
                name: DELETE_NAME,
                value: "true"
            }).appendTo($form);
        }
    }

    function removeDeleteInput($form) {
        $form.find("input[name='" + DELETE_NAME + "']").remove();
    }

    function sync($scope) {
        var $form = $scope.closest("form").add($scope.find("form")).first();
        if (isChecked($scope)) {
            setCssEnabled($scope, true);
            removeDeleteInput($form);
        } else {
            clearCssValue($scope);
            setCssEnabled($scope, false);
            ensureDeleteInput($form);
        }
    }

    function bind($scope) {
        if (!$scope.length || $scope.data("singlearticle-bound")) {
            return;
        }
        if (!getCheckbox($scope).length) {
            return;
        }
        $scope.data("singlearticle-bound", true);
        sync($scope);
        $scope.on("change", "coral-checkbox[name='" + CHECKBOX_NAME + "'], input[name='" + CHECKBOX_NAME + "']", function () {
            sync($scope);
        });
    }

    $(document).on("foundation-contentloaded", function (event) {
        bind(findDialog(event.target));
        bind($(event.target));
    });

    $(document).on("submit", "form.cq-dialog", function () {
        var $form = $(this);
        if (!getCheckbox($form).length) {
            return;
        }
        if (isChecked($form)) {
            removeDeleteInput($form);
            setCssEnabled($form, true);
        } else {
            clearCssValue($form);
            setCssEnabled($form, true);
            getCssInput($form).val("");
            setCssEnabled($form, false);
            ensureDeleteInput($form);
        }
    });
})(document, Granite.$);
