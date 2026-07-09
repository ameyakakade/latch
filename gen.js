const mcu = require('./mcu.js'); 
const theme = mcu.themeFromSourceColor(mcu.argbFromHex('#FF6B6B'), [{name: 'secondary', value: mcu.argbFromHex('#073691'), blend: false}]); 
const light = {}; 
const dark = {}; 
for(const k of Object.keys(theme.schemes.light.toJSON())){
  light[k] = mcu.hexFromArgb(theme.schemes.light.toJSON()[k]); 
  dark[k] = mcu.hexFromArgb(theme.schemes.dark.toJSON()[k]);
} 
console.log('LIGHT:', JSON.stringify(light, null, 2)); 
console.log('DARK:', JSON.stringify(dark, null, 2));
