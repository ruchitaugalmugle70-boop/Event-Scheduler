const http = require('http');

http.get('http://localhost:8080/solve?method=backtracking', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        if(data.includes('startReplay()')) {
            console.log("HTML contains startReplay");
        }
        if(data.includes('vis.DataSet()')) {
            console.log("HTML contains vis.DataSet");
        }
        // Let's dump all script tags
        let scripts = data.match(/<script[\s\S]*?<\/script>/gi);
        scripts.forEach((s, i) => console.log("Script", i, s.substring(0, 100).replace(/\n/g, ' ')));
    });
}).on('error', (err) => console.log("Error:", err.message));
