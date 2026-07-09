import { themeFromSourceColor, argbFromHex, hexFromArgb } from '@material/material-color-utilities';

const theme = themeFromSourceColor(argbFromHex('#FF6B6B'), [
  { name: 'secondary', value: argbFromHex('#073691'), blend: false }
]);

const light = {};
const dark = {};

for (const [key, value] of Object.entries(theme.schemes.light.toJSON())) {
  light[key] = hexFromArgb(value);
}

for (const [key, value] of Object.entries(theme.schemes.dark.toJSON())) {
  dark[key] = hexFromArgb(value);
}

console.log("LIGHT:", JSON.stringify(light, null, 2));
console.log("DARK:", JSON.stringify(dark, null, 2));
