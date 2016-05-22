var d = $("body").append("<div></div>");

function update_page(str) {
    console.log(str);
    $(".profile-sidebar").attr("style", "visibility:hidden");
    d.addClass('spinner');
    d3.select("svg").remove();
    d3.select("svg.wordcloud").remove();
    var url = "http://localhost:8083/update/" + str;
    d3.xhr(url, function(error, data) {
        console.log(data);
        d.removeClass('spinner');
        begin();

    });
}

function clear_matrix() {
    $(".profile-sidebar").attr("style", "visibility:hidden");
    d.addClass('spinner');
    d3.select("svg").remove();
    d3.select("svg.wordcloud").remove();
    var url = "http://localhost:8083/clear";
    d3.xhr(url, function(error, data) {
        console.log(data);
        d.removeClass('spinner');
        begin();

    });

}

function begin() {
    d.addClass('spinner');
    var margin = {
            top: 20,
            right: 20,
            bottom: 30,
            left: 40
        },
        width = 960 - margin.left - margin.right,
        height = 500 - margin.top - margin.bottom;

    var x = d3.scale.linear()
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);

    var color = d3.scale.category10();

    var xAxis = d3.svg.axis()
        .scale(x)
        .orient("bottom");

    var yAxis = d3.svg.axis()
        .scale(y)
        .orient("left");

    var svg = d3.select("body").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // Lasso functions to execute while lassoing
    var lasso_start = function() {
        lasso.items()
            .attr("r", 3.5) // reset size
            .style("fill", null) // clear all of the fills
            .classed({
                "not_possible": true,
                "selected": false
            }); // style as not possible
    };

    var lasso_draw = function() {
        // Style the possible dots
        lasso.items().filter(function(d) {
                return d.possible === true
            })
            .classed({
                "not_possible": false,
                "possible": true
            });

        // Style the not possible dot
        lasso.items().filter(function(d) {
                return d.possible === false
            })
            .classed({
                "not_possible": true,
                "possible": false
            });
    };

    var lasso_end = function() {
        // Reset the color of all dots
        lasso.items()
            .style("fill", function(d) {
                return color(d.species);
            });

        // Style the selected dots
        lasso.items().filter(function(d) {
                return d.selected === true
            })
            .classed({
                "not_possible": false,
                "possible": false
            })
            .attr("r", 7);

        // Reset the style of the not selected dots
        lasso.items().filter(function(d) {
                return d.selected === false
            })
            .classed({
                "not_possible": false,
                "possible": false
            })
            .attr("r", 3.5);

    };

    // Create the area where the lasso event can be triggered
    var lasso_area = svg.append("rect")
        .attr("width", width)
        .attr("height", height)
        .style("opacity", 0);

    // Define the lasso
    var lasso = d3.lasso()
        .closePathDistance(75) // max distance for the lasso loop to be closed
        .closePathSelect(true) // can items be selected by closing the path?
        .hoverSelect(true) // can items by selected by hovering over them?
        .area(lasso_area) // area where the lasso can be started
        .on("start", lasso_start) // lasso start function
        .on("draw", lasso_draw) // lasso draw function
        .on("end", lasso_end); // lasso end function

    d3.xhr("http://localhost:8083/initial_corordinates", function(error, data) {
        var data = JSON.parse(data.response);
        var word_cloud = data.word_cloud;
        console.log(word_cloud)
        var word_cloud_cluster = {}
        for (var i = word_cloud.length - 1; i >= 0; i--) {
            word_cloud_cluster[word_cloud[i].text] = word_cloud[i].cluster;
        }
        // console.log(word_cloud_cluster);
        d.removeClass('spinner');
        $(".profile-sidebar").attr("style", "visibility:visible");
        // console.log(word_cloud);

        function main(new_data) {
            svg.call(lasso);
            d3.selectAll(".panel-body").on("mouseup", function() {
                    var selected = checkSelection();
                    var str = selected.replace(" ","_")
                    $(".profile-sidebar").attr("style", "visibility:hidden");
                    d.addClass('spinner');
                    d3.select("svg").remove();
                    d3.select("svg.wordcloud").remove();
                    var url = "http://localhost:8083/update/" + str;
                    d3.xhr(url, function(error, data) {
                        console.log(data);
                        d.removeClass('spinner');
                        begin();

                    });


                    console.log(selected);
                })
                .on("keyup", function() {
                    var selected = checkSelection();
                    console.log(selected);
                });
            // new_data.forEach(function(d) {
            //   d.sepalLength = +d.sepalLength;
            //   d.sepalWidth = +d.sepalWidth;
            // });

            x.domain(d3.extent(new_data, function(d) {
                return d.x;
            })).nice();
            y.domain(d3.extent(new_data, function(d) {
                return d.y;
            })).nice();

            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis)
                .append("text")
                .attr("class", "label")
                .attr("x", width)
                .attr("y", -6)
                .style("text-anchor", "end")
                .text("X axis");

            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("class", "label")
                .attr("transform", "rotate(-90)")
                .attr("y", 6)
                .attr("dy", ".71em")
                .style("text-anchor", "end")
                .text("Y axis")

            var drag = d3.behavior.drag()
                .on("dragstart", dragstarted)
                .on("drag", dragmove)
                .on("dragend", dragended);

            nodes = svg.selectAll(".dot")
                .data(new_data)
                .enter().append("circle")
                .attr("id", function(d, i) {
                    return "dot_" + i;
                }) // added
                .attr("class", "dot")
                .attr("r", 3.5)
                .attr("cx", function(d) {
                    return x(d.x);
                })
                .attr("cy", function(d) {
                    return y(d.y);
                })
                .style("fill", function(d) {
                    return color(d.cluster);
                })
                .on("click", click)
                .call(drag);

            lasso.items(d3.selectAll(".dot"));

            var legend = svg.selectAll(".legend")
                .data(color.domain())
                .enter().append("g")
                .attr("class", "legend")
                .attr("transform", function(d, i) {
                    return "translate(0," + i * 20 + ")";
                });

            legend.append("rect")
                .attr("x", width - 18)
                .attr("width", 18)
                .attr("height", 18)
                .style("fill", color);

            legend.append("text")
                .attr("x", width - 24)
                .attr("y", 9)
                .attr("dy", ".35em")
                .style("text-anchor", "eventnd")
                .text(function(d) {
                    return d;
                });
        };

        main(data.coordinates);

        function click(d) {
            $(".panel-body").text(d.synopsis)
            $(".panel-title").text(d.name)
        };

        function dragstarted(d) {};

        function dragmove(d) {
            d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
        };

        function dragended(d) {};

        // var frequency_list = [{"text":"study","size":40},{"text":"motion","size":15},{"text":"forces","size":10},{"text":"electricity","size":15},{"text":"movement","size":10},{"text":"relation","size":5},{"text":"things","size":10},{"text":"force","size":5},{"text":"ad","size":5},{"text":"energy","size":85},{"text":"living","size":5},{"text":"nonliving","size":5},{"text":"laws","size":15},{"text":"speed","size":45},{"text":"velocity","size":30},{"text":"define","size":5},{"text":"constraints","size":5},{"text":"universe","size":10},{"text":"physics","size":120},{"text":"describing","size":5},{"text":"matter","size":90},{"text":"physics-the","size":5},{"text":"world","size":10},{"text":"works","size":10},{"text":"science","size":70},{"text":"interactions","size":30},{"text":"studies","size":5},{"text":"properties","size":45},{"text":"nature","size":40},{"text":"branch","size":30},{"text":"concerned","size":25},{"text":"source","size":40},{"text":"google","size":10},{"text":"defintions","size":5},{"text":"two","size":15},{"text":"grouped","size":15},{"text":"traditional","size":15},{"text":"fields","size":15},{"text":"acoustics","size":15},{"text":"optics","size":15},{"text":"mechanics","size":20},{"text":"thermodynamics","size":15},{"text":"electromagnetism","size":15},{"text":"modern","size":15},{"text":"extensions","size":15},{"text":"thefreedictionary","size":15},{"text":"interaction","size":15},{"text":"org","size":25},{"text":"answers","size":5},{"text":"natural","size":15},{"text":"objects","size":5},{"text":"treats","size":10},{"text":"acting","size":5},{"text":"department","size":5},{"text":"gravitation","size":5},{"text":"heat","size":10},{"text":"light","size":10},{"text":"magnetism","size":10},{"text":"modify","size":5},{"text":"general","size":10},{"text":"bodies","size":5},{"text":"philosophy","size":5},{"text":"brainyquote","size":5},{"text":"words","size":5},{"text":"ph","size":5},{"text":"html","size":5},{"text":"lrl","size":5},{"text":"zgzmeylfwuy","size":5},{"text":"subject","size":5},{"text":"distinguished","size":5},{"text":"chemistry","size":5},{"text":"biology","size":5},{"text":"includes","size":5},{"text":"radiation","size":5},{"text":"sound","size":5},{"text":"structure","size":5},{"text":"atoms","size":5},{"text":"including","size":10},{"text":"atomic","size":10},{"text":"nuclear","size":10},{"text":"cryogenics","size":10},{"text":"solid-state","size":10},{"text":"particle","size":10},{"text":"plasma","size":10},{"text":"deals","size":5},{"text":"merriam-webster","size":5},{"text":"dictionary","size":10},{"text":"analysis","size":5},{"text":"conducted","size":5},{"text":"order","size":5},{"text":"understand","size":5},{"text":"behaves","size":5},{"text":"en","size":5},{"text":"wikipedia","size":5},{"text":"wiki","size":5},{"text":"physics-","size":5},{"text":"physical","size":5},{"text":"behaviour","size":5},{"text":"collinsdictionary","size":5},{"text":"english","size":5},{"text":"time","size":35},{"text":"distance","size":35},{"text":"wheels","size":5},{"text":"revelations","size":5},{"text":"minute","size":5},{"text":"acceleration","size":20},{"text":"torque","size":5},{"text":"wheel","size":5},{"text":"rotations","size":5},{"text":"resistance","size":5},{"text":"momentum","size":5},{"text":"measure","size":10},{"text":"direction","size":10},{"text":"car","size":5},{"text":"add","size":5},{"text":"traveled","size":5},{"text":"weight","size":5},{"text":"electrical","size":5},{"text":"power","size":5}];


        // var color1 = d3.scale.linear()
        //         .domain([0,1,2,3,4,5,6,10,15,20,100])
        //         .range(["#ddd", "#ccc", "#bbb", "#aaa", "#999", "#888", "#777", "#666", "#555", "#444", "#333", "#222"]);

        d3.layout.cloud().size([800, 200])
            .words(word_cloud)
            // .rotate(function() { return ~~(Math.random() * 2) * 90; })
            .rotate(0)
            .fontSize(function(d) {
                return d.size * 200;
            })
            .on("end", draw)
            .start();

        function draw(words) {
            console.log(word_cloud);
            d3.select("body").append("svg")
                .attr("width", 850)
                .attr("height", 270)
                .attr("class", "wordcloud")
                .append("g")
                // without the transform, words words would get cutoff to the left and top, they would
                // appear outside of the SVG area
                .attr("transform", "translate(400,100)")
                .selectAll("text")
                .data(words)
                .enter().append("text")
                .style("font-size", function(d) {
                    return (d.size) + "px";
                })
                .style("fill", function(d, i) {
                    console.log(d);
                    return color(word_cloud_cluster[d.text]);
                })
                .attr("transform", function(d) {
                    return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
                })
                .text(function(d) {
                    return d.text;
                })
                .on("dblclick", function(d) {
                    update_page(d.text);
                    // console.log(d);
                });
        }

    });
}
begin();
// setTimeout(function() {
// d3.select("svg").remove();
// d3.select("svg.wordcloud").remove();
// },500)