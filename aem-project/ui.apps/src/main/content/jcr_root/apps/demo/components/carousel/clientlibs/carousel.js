(function () {
    "use strict";

    var SELECTOR = ".hof-wrapper";
    var carousels = [];
    var resizeBound = false;

    function stepSize(scroller) {
        var card = scroller.querySelector(".hof-card");
        if (!card) {
            return scroller.clientWidth;
        }
        var styles = window.getComputedStyle(scroller);
        var gap = parseFloat(styles.columnGap);
        if (isNaN(gap)) {
            gap = parseFloat(styles.gap) || 0;
        }
        return card.getBoundingClientRect().width + gap;
    }

    function update(carousel) {
        var scroller = carousel.scroller;
        var max = scroller.scrollWidth - scroller.clientWidth;
        var left = scroller.scrollLeft;

        if (carousel.prev) {
            carousel.prev.disabled = left <= 2;
        }
        if (carousel.next) {
            carousel.next.disabled = max <= 2 || left >= max - 2;
        }
    }

    function scroll(carousel, direction) {
        if (carousel.moving) {
            return;
        }
        carousel.moving = true;
        carousel.scroller.scrollBy({
            left: direction * stepSize(carousel.scroller),
            behavior: "smooth"
        });
        window.setTimeout(function () {
            carousel.moving = false;
            update(carousel);
        }, 450);
    }

    function init(root) {
        if (root.getAttribute("data-hof-ready") === "true") {
            return;
        }

        var scroller = root.querySelector(".hof-item");
        var prev = root.querySelector(".hof-arrow--prev");
        var next = root.querySelector(".hof-arrow--next");

        if (!scroller || !scroller.querySelector(".hof-card")) {
            return;
        }

        root.setAttribute("data-hof-ready", "true");

        var carousel = {
            root: root,
            scroller: scroller,
            prev: prev,
            next: next,
            moving: false
        };

        if (prev) {
            prev.addEventListener("click", function () {
                scroll(carousel, -1);
            });
        }
        if (next) {
            next.addEventListener("click", function () {
                scroll(carousel, 1);
            });
        }

        scroller.addEventListener("scroll", function () {
            update(carousel);
        });
        scroller.addEventListener("scrollend", function () {
            carousel.moving = false;
            update(carousel);
        });

        carousels.push(carousel);
        update(carousel);
    }

    function boot(scope) {
        var nodes = (scope || document).querySelectorAll(SELECTOR);
        Array.prototype.forEach.call(nodes, init);
    }

    function onResize() {
        carousels = carousels.filter(function (carousel) {
            return carousel.root.isConnected;
        });
        carousels.forEach(update);
    }

    function start() {
        boot(document);

        if (!resizeBound) {
            resizeBound = true;
            window.addEventListener("resize", onResize);
        }

        if (!document.body || typeof MutationObserver !== "function") {
            return;
        }

        var observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                Array.prototype.forEach.call(mutation.addedNodes, function (node) {
                    if (!node || node.nodeType !== 1) {
                        return;
                    }
                    if (node.matches && node.matches(SELECTOR)) {
                        init(node);
                    }
                    if (node.querySelectorAll) {
                        boot(node);
                    }
                });
            });
        });

        observer.observe(document.body, { childList: true, subtree: true });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
}());
