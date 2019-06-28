const fs = require('fs');

console.log("XXXXXXXX");

let TYPE = new Array("Vertices","Edges")

for(let index = 1; index <=5; index++){

    let strALL = "";
    for(let k = 0; k <=1; k++) {

        const filename = "com-youtube.txt_@_#"
            .replace("@", index + "" + index + "" + index + "" + index + "")
            .replace("#", TYPE[k]);

        console.log(filename);
        strALL += fs.readFileSync("I:\\IDEA_PROJ\\Visualization\\output\\OUTPUT\\" + filename + "\\part-00000", "utf8");
        strALL += fs.readFileSync("I:\\IDEA_PROJ\\Visualization\\output\\OUTPUT\\" + filename + "\\part-00001", "utf8");


        let nodes = {};
        let links = {};
        if(TYPE[k] == "Vertices") {

            let lines = strALL.split("\n");
            for (let t = 0; t < lines.length; t++) {
                let eachline = lines[t].split(",");
                let nd = {};
                nd.weight = "0";
                nd.name = eachline[0].replace("(");
                nd.value = "1";
                nd.cx = eachline[2];
                nd.cy = eachline[3];
                nodes.push(nd)
                //toFIle += '{"weight": "0","name+": "o","value": "1", "cx":"0","cy": "0"}';
            }

        }else{
            let lines = strALL.split("\n");
            for (let t = 0; t < lines.length; t++) {
                let eachline = lines[t].split(",");
                let v1 = eachline[0].replace("Edge(");
                let v2 = eachline[1];
                let node1 = nodes.find(function (obj){ if(obj.name== v1){ return obj; } });
                let node2 = nodes.find(function (obj){ if(obj.name== v2){ return obj; } });
                let lk = {};
                lk.value = "1";
                lk.cx = node1.cx
                lk.cy = eachline[3];
                links.push(lk)
                //"{"value": "0","x1": "${x.srcAttr._2}","y1": "${x.srcAttr._3}","x2": "${x.dstAttr._2}","y2": "${x.dstAttr._3}"},"
            }
        }



        let toFIle = ""
        fs.writeFile("I:\\IDEA_PROJ\\Visualization\\output\\OUTPUT\\" + filename + ".json",
            toFIle, {'flag': 'a'}, function (err) {
            if (err) {
                throw err;
            }
        });


    }


}



p => {
    p.println("""{"nodes": [""")
    g.vertices.collect.foreach(
        x => p.println(s"""{"weight": "0","name": "o","value": "${x._2._4._3/10f}","cx":"${x._2._2}","cy": "${x._2._3}"}, """)
)
    p.println("""{"weight": "0","name": "o","value": "0","cx":"0","cy": "0"}],    "links": [""")
    g.triplets.collect.foreach(
        x => p.println(s"""{"value": "0","x1": "${x.srcAttr._2}","y1": "${x.srcAttr._3}","x2": "${x.dstAttr._2}","y2": "${x.dstAttr._3}"},""")
)
    p.println("""{"value": "0","x1": "0","y1": "0","x2": "0","y2": "0"}]}""")
}