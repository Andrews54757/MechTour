

const fs = require('fs');
const originalFile = fs.readFileSync('./waypoints.points', 'utf8');


const split = originalFile.split('\n');


const parsed = split.map(l=>l.trim()).filter(l=>l.length).map(line => {
    const entries = line.split(",");
    const obj = {}
    entries.forEach((entry)=>{
        let pair = entry.split(":");
        obj[pair[0]] = pair.slice(1).join(":");
    })
    return obj;
});

const results = [];

parsed.slice(3).forEach((obj)=>{
    obj.dimensions.split("#").forEach((dim)=>{
        if (!dim.length) return;
        var result = {
            name: obj.name,
            dimension: dim,
            icon: obj.suffix || "waypoint",
            x: parseInt(obj.x),
            y: parseInt(obj.y),
            z: parseInt(obj.z),
            r: Math.floor(parseFloat(obj.red) * 255),
            g: Math.floor(parseFloat(obj.green) * 255),
            b: Math.floor(parseFloat(obj.blue) * 255)
        }
        if (dim === "the_nether") {
            result.x = Math.floor(result.x / 8);
            result.z = Math.floor(result.z / 8);
        }

        results.push(result);
    });
});

console.log("Converted " + results.length + " waypoints");

fs.writeFileSync('./waypoints.json', JSON.stringify(results, null, 2));