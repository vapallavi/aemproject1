(function () {
    "use strict";

    var DESKTOP_MQ = "(min-width: 1024px)";
    var CLOSE_DELAY = 140;
    var STICKY_HOPS = 6;

    function isDesktop() {
        return window.matchMedia(DESKTOP_MQ).matches;
    }

    function promoteSticky(header) {
        var node = header.parentElement;
        var hops = 0;
        while (node && hops < STICKY_HOPS) {
            var cls = node.className || "";
            if (typeof cls === "string" &&
                (cls.indexOf("experiencefragment") !== -1 ||
                    cls.indexOf("cmp-experiencefragment") !== -1 ||
                    cls.indexOf("xf-content-height") !== -1 ||
                    cls.indexOf("hof-header-root") !== -1)) {
                node.classList.add("hof-header-sticky-host");
            }
            node = node.parentElement;
            hops += 1;
        }
    }

    function setExpanded(el, expanded) {
        if (el) {
            el.setAttribute("aria-expanded", expanded ? "true" : "false");
        }
    }

    function closeMega(item) {
        if (!item) {
            return;
        }
        item.classList.remove("is-open");
        var trigger = item.querySelector("[data-hof-mega-trigger]");
        var panel = item.querySelector(".hof-header__mega");
        setExpanded(trigger, false);
        if (panel) {
            panel.setAttribute("aria-hidden", "true");
        }
    }

    function openMega(item) {
        if (!item) {
            return;
        }
        item.classList.add("is-open");
        var trigger = item.querySelector("[data-hof-mega-trigger]");
        var panel = item.querySelector(".hof-header__mega");
        setExpanded(trigger, true);
        if (panel) {
            panel.setAttribute("aria-hidden", "false");
        }
    }

    function closeAllMegas(header, except) {
        var items = header.querySelectorAll("[data-hof-mega-item]");
        for (var i = 0; i < items.length; i += 1) {
            if (items[i] !== except) {
                closeMega(items[i]);
            }
        }
    }

    function setMenuOpen(header, open) {
        var nav = header.querySelector("[data-hof-nav]");
        var toggle = header.querySelector("[data-hof-menu-toggle]");
        if (nav) {
            nav.classList.toggle("is-open", open);
        }
        setExpanded(toggle, open);
        if (toggle) {
            toggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
        }
        if (!open) {
            closeAllMegas(header);
        }
    }

    function bindHeader(header) {
        if (header.getAttribute("data-hof-ready") === "true") {
            return;
        }
        header.setAttribute("data-hof-ready", "true");
        promoteSticky(header);

        var menuToggle = header.querySelector("[data-hof-menu-toggle]");
        var megaItems = header.querySelectorAll("[data-hof-mega-item]");
        var closeTimer = null;

        function cancelClose() {
            if (closeTimer) {
                window.clearTimeout(closeTimer);
                closeTimer = null;
            }
        }

        function scheduleClose(item) {
            cancelClose();
            closeTimer = window.setTimeout(function () {
                if (isDesktop()) {
                    closeMega(item);
                }
            }, CLOSE_DELAY);
        }

        if (menuToggle) {
            menuToggle.addEventListener("click", function () {
                var open = menuToggle.getAttribute("aria-expanded") !== "true";
                setMenuOpen(header, open);
            });
        }

        for (var i = 0; i < megaItems.length; i += 1) {
            (function (item) {
                var trigger = item.querySelector("[data-hof-mega-trigger]");

                item.addEventListener("pointerenter", function () {
                    if (!isDesktop()) {
                        return;
                    }
                    cancelClose();
                    closeAllMegas(header, item);
                    openMega(item);
                });

                item.addEventListener("pointerleave", function () {
                    if (isDesktop()) {
                        scheduleClose(item);
                    }
                });

                item.addEventListener("focusin", function () {
                    if (isDesktop()) {
                        cancelClose();
                        closeAllMegas(header, item);
                        openMega(item);
                    }
                });

                item.addEventListener("focusout", function (event) {
                    if (!isDesktop()) {
                        return;
                    }
                    if (!item.contains(event.relatedTarget)) {
                        scheduleClose(item);
                    }
                });

                if (trigger) {
                    trigger.addEventListener("click", function (event) {
                        var open = item.classList.contains("is-open");
                        if (isDesktop() && open) {
                            event.preventDefault();
                            closeMega(item);
                            return;
                        }
                        if (!isDesktop()) {
                            event.preventDefault();
                            if (open) {
                                closeMega(item);
                            } else {
                                closeAllMegas(header, item);
                                openMega(item);
                            }
                        }
                    });
                }
            }(megaItems[i]));
        }

        document.addEventListener("keydown", function (event) {
            if (event.key !== "Escape") {
                return;
            }
            closeAllMegas(header);
            setMenuOpen(header, false);
        });

        document.addEventListener("click", function (event) {
            if (!header.contains(event.target)) {
                closeAllMegas(header);
                if (!isDesktop()) {
                    setMenuOpen(header, false);
                }
            }
        });

        window.addEventListener("resize", function () {
            if (isDesktop()) {
                setMenuOpen(header, false);
            } else {
                closeAllMegas(header);
            }
        });
    }

    function init() {
        var headers = document.querySelectorAll("[data-hof-header]");
        for (var i = 0; i < headers.length; i += 1) {
            bindHeader(headers[i]);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
}());
