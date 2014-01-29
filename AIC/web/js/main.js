Handlebars.registerHelper('join', function(array) {
    return array.join(" ");
});

$(function() {
    var token = "";
    var user = "";
    var taskReload;
    var auth = function() {
        return token.length == 32;
    };

    var fForms = {
        "form-signin": function(e) {
            e.preventDefault();

            document.activeElement.blur();

            token = "";
            user = this[0].value;
            var save = this[2].checked;

            $.ajax({
                url: rest.login(),
                type: "post",
                data: {
                    name: this[0].value,
                    pass: this[1].value
                }
            }).always(function(data) {
                var temp = data.responseText != undefined ? data.responseText : data;

                if (temp.length == 32) {
                    token = temp;

                    if (save) {
                        localStorage.setItem("token", token);
                    }

                    rest.query.token(token);
                    rest.tasks.token(token);
                    rest.subscribe.token(token);
                    load("tasks");
                }
            });
        },
        "form-subscribe": function(e) {
            if (auth()) {
                $.ajax({
                    url: rest.subscribe(),
                    data: {
                        token: token,
                        topic: this[0].value,
                        time: this[1].value
                    }
                }).always(function() {
                    load("tasks");
                });
            }
        },
        "form-register": function(e) {
            $.ajax({
                url: rest.register(),
                data: {
                    name: this[0].value,
                    pass: this[1].value
                },
                method: "post"
            }).done(function(data) {
                if (data == "true") {
                    load("login");
                } else {
                    alert("An error occured!");
                }
            });
        }
    };

    var r = function(query, args) {
        var base = "http://localhost:8080/api/" + query;
        var values = {};
        var self = function(params) {
            var url = base;
            var params = params || {};

            args.forEach(function(o) {
                var param = params[o] || values[o];

                if (param != null) {
                    url += (url.indexOf("?") > -1 ? "&" : "?") + o + "=" + values[o];
                }
            });

            return url;
        };

        args.forEach(function(o) {
            values[o] = null;

            self[o] = (function(x) {
                return function(val) {
                    values[o] = val;
                };
            })(o);
        });

        return self;
    }

    var rest = {
        base: "http://localhost:8080/api/",
        configurations: new r("configurations", ["token"]),
        login: new r("login", ["name", "pass"]),
        logout: new r("logout", ["token"]),
        query: new r("query", ["token", "task", "config"]),
        register: new r("register", ["name", "pass"]),
        tasks: new r("tasks", ["token"]),
        subscribe: new r("subscribe", ["token", "topic", "time"])
    };

    var configurations = [];

    var $overlay = $(".overlay");
    var $container = $(".content");

    var showOverlay = function() {
        $overlay.removeClass("hidden").addClass("opaque");
    };

    var hideOverlay = function() {
        $overlay.removeClass("opaque");

        setTimeout(function() {
            $overlay.addClass("hidden");
        }, 500);
    }

    var currentLocation = "";
    var f = {};
    var tweets = [];

    var load = function(hash, callback, x) {
        if (hash == currentLocation) {
            return;
        }

        if (tweets.length == 0 && hash == "tweets") {
            hash = "login";
        }

        if (!auth() && hash != "login" && hash != "register") {
            document.location.hash = "login";
            hideOverlay();
            alert("You need to login to do this!");
            return;
        }

        clearInterval(taskReload);
        document.location.hash = hash;
        currentLocation = hash;
        var url = "templates/" + hash + ".html";
        showOverlay();

        $.get(url, function(data) {
            data = callback != null ? callback(data) : data;

            if ($("#" + hash).length > 0) {
                $(".active").removeClass("active");
                $("#" + hash).addClass("active");
            }

            if (typeof f[hash] == "function") {
                f[hash](data, x);
            } else {
                $container.children().remove();
                $container.append(data);
                hideOverlay();
            }
        });
    };

    f.tasks = function(html) {
        var template = Handlebars.compile(html);
        var reload = function() {
            $.get(rest.tasks(), function(data) {
                $container.html(template({
                    subscriptions: data
                }));
            });
        };

        taskReload = setInterval(function() {
            reload();
        }, 1000);

        reload();
        hideOverlay();
    };

    f.logout = function() {
        $.get(rest.logout({
            token: token
        }));

        token = "";
        user = "";
        localStorage.setItem("token", "");
        load("login");
    }

    f.tweets = function(html) {
        var template = Handlebars.compile(html);

        $container.children().remove();
        $container.append(template(tweets));
        hideOverlay();
    };

    f.query = function(html, x) {
        var template = Handlebars.compile(html);

        $container.children().remove();
        $container.append(template($.extend(configurations, x)));
        hideOverlay();
    };

    var hash = function() {
        var hash = document.location.hash.replace("#", "");

        if (hash == "") {
            return "login";
        }

        return hash;
    }

    $(document).on("submit", "form", function(e) {
        if (fForms[e.target.id] != undefined) {
            fForms[e.target.id].call(this, e);
        }
    }).on("click", "#logout", function(e) {
        e.preventDefault();
        f.logout();
    }).on("click", ".btn-query", function(e) {
        e.preventDefault();

        load("query", null, {
            topic: $(this).data("topic"),
            id: e.target.id.replace("query-", "")
        });
    }).on("change", "input[type=radio]", function() {
        $("label.active").removeClass("active");
        $(this).parent().addClass("active");
    }).on("click", ".btn-tweets", function(e) {
        e.preventDefault();

        if ($("input[name=config]:checked").val() != undefined) {
            showOverlay();

            rest.query.token(token);
            rest.query.task(e.target.id);
            rest.query.config($("input[name=config]:checked").val());

            $.ajax({
                url: rest.query()
            }).done(function(data) {
                tweets = data;
                load("tweets");
            });
        } else {
            alert("You have to select a configuration!");
        }
    });

    token = localStorage.getItem("token") || "";

    if (token != "") {
        rest.query.token(token);
        rest.tasks.token(token);
        rest.subscribe.token(token);
        load("tasks");
    } else {
        load(hash());
    }

    $.get(rest.configurations()).done(function(data) {
        configurations = data;
    });

    $(window).on("hashchange", function(e) {
        load(hash());
    }).trigger("hashchange");
});
