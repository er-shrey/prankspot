package com.shrey.prankspot.portal

import com.shrey.prankspot.domain.PrankTemplate

/**
 * Turns a prank template into one self-contained HTML page: inline CSS and JS only, no external
 * requests, no forms, nothing collected. The page always ends by saying it was a prank.
 */
object PageRenderer {
    private val animations = setOf("loading", "confetti", "shake", "ticket", "countdown", "siren", "matrix", "float", "snow")

    fun render(
        prank: PrankTemplate,
        customTitle: String = "",
        customMessage: String = "",
        sound: Boolean = true,
    ): String {
        val title = customTitle.ifBlank { prank.title }
        val message = customMessage.ifBlank { prank.message }
        val anim = prank.animation.takeIf { it in animations } ?: "loading"
        val extras = prank.extras.joinToString(",", "[", "]") { js(it) }
        return """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; img-src data:">
<title>${html(title)}</title>
<style>
*{box-sizing:border-box}
html{min-height:100%;background-color:${css(prank.bgStart)};
 background-image:linear-gradient(160deg,${css(prank.bgStart)},${css(prank.bgEnd)});background-attachment:fixed}
body{margin:0;min-height:100vh;min-height:100dvh;display:flex;align-items:center;justify-content:center;padding:24px 16px;
 font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;color:${css(prank.textColor)};overflow-x:hidden}
#fx{position:fixed;inset:0;pointer-events:none;z-index:0}
.card{position:relative;z-index:1;width:100%;max-width:440px;text-align:center;padding:28px 22px;border-radius:24px;
 background:rgba(0,0,0,.22);backdrop-filter:blur(6px);box-shadow:0 12px 40px rgba(0,0,0,.25)}
.emoji{font-size:72px;line-height:1;margin-bottom:8px;display:inline-block;animation:bob 2.4s ease-in-out infinite}
h1{font-size:clamp(24px,7vw,34px);margin:8px 0 12px;line-height:1.15}
p{font-size:18px;line-height:1.45;margin:8px 0}
#extra{min-height:52px;opacity:.92;transition:opacity .4s}
.bar{height:14px;border-radius:7px;background:rgba(255,255,255,.25);overflow:hidden;margin:18px 0 6px}
.bar>div{height:100%;width:0;background:${css(prank.accent)};transition:width .5s}
#pct{font-size:14px;opacity:.8}
#count{font-size:64px;font-weight:800;color:${css(prank.accent)};margin:10px 0}
.ticket{margin:16px auto;padding:14px;border:2px dashed ${css(prank.accent)};border-radius:14px;font-family:ui-monospace,monospace;font-size:15px}
button{margin-top:18px;font:inherit;font-weight:700;font-size:18px;border:0;border-radius:999px;padding:14px 26px;
 background:${css(prank.accent)};color:#1a1a1a;cursor:pointer;box-shadow:0 6px 18px rgba(0,0,0,.25)}
button:active{transform:scale(.96)}
#reveal{display:none;margin-top:18px;font-size:20px;font-weight:700}
.foot{margin-top:22px;font-size:12px;opacity:.65}
.shake{animation:shake .5s}
.siren{animation:siren 1s steps(2) infinite}
@keyframes bob{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}
@keyframes shake{0%,100%{transform:translateX(0)}20%,60%{transform:translateX(-10px)}40%,80%{transform:translateX(10px)}}
@keyframes siren{0%{box-shadow:0 0 60px 10px rgba(255,0,0,.6)}100%{box-shadow:0 0 60px 10px rgba(0,80,255,.6)}}
@media (prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}}
</style>
</head>
<body data-anim="$anim">
<canvas id="fx"></canvas>
<main class="card" id="card">
 <div class="emoji">${html(prank.emoji)}</div>
 <h1>${html(title)}</h1>
 <p>${html(message)}</p>
 <div id="widget"></div>
 <p id="extra"></p>
 <button id="go" type="button">${html(prank.button)}</button>
 <p id="reveal">${html(prank.reveal)}</p>
 <div class="foot">A harmless PrankSpot prank. Nothing is collected from your device.</div>
</main>
<script>
(function(){
var EXTRAS=$extras, ANIM=${js(anim)}, SOUND=${if (sound) js(prank.sound) else "null"}, ACCENT=${js(prank.accent)};
var w=document.getElementById('widget'), ex=document.getElementById('extra'), card=document.getElementById('card');
var cv=document.getElementById('fx'), cx=cv.getContext('2d'), parts=[], mode=null;
function size(){cv.width=innerWidth;cv.height=innerHeight}size();addEventListener('resize',size);
var i=0;function nextExtra(){if(!EXTRAS.length)return;ex.style.opacity=0;setTimeout(function(){ex.textContent=EXTRAS[i++%EXTRAS.length];ex.style.opacity=.92},350)}
nextExtra();setInterval(nextExtra,3500);
function rnd(a,b){return a+Math.random()*(b-a)}
function burst(n){mode='confetti';var c=['#FFD54F','#FF4081','#40C4FF','#69F0AE','#FFFFFF',ACCENT];
 for(var k=0;k<n;k++)parts.push({x:rnd(0,cv.width),y:rnd(-cv.height,0),vx:rnd(-1,1),vy:rnd(2,5),r:rnd(4,8),c:c[k%c.length],a:rnd(0,6)})}
function flakes(sym,n){for(var k=0;k<n;k++)parts.push({x:rnd(0,cv.width),y:rnd(-cv.height,cv.height),vx:rnd(-.5,.5),vy:rnd(.6,2),r:rnd(14,26),s:sym,a:0})}
var cols=[];function matrix(){mode='matrix';cols=[];for(var k=0;k<cv.width/16;k++)cols.push(rnd(-50,0))}
function frame(){
 if(mode==='matrix'){cx.fillStyle='rgba(0,0,0,.08)';cx.fillRect(0,0,cv.width,cv.height);cx.fillStyle='#00FF41';cx.font='15px monospace';
  for(var k=0;k<cols.length;k++){cx.fillText(String.fromCharCode(0x30A0+Math.random()*96),k*16,cols[k]*16);if(cols[k]*16>cv.height&&Math.random()>.97)cols[k]=0;cols[k]++}}
 else{cx.clearRect(0,0,cv.width,cv.height);
  for(var k=0;k<parts.length;k++){var p=parts[k];p.x+=p.vx;p.y+=p.vy;p.a+=.1;
   if(p.s){cx.font=p.r+'px serif';cx.fillText(p.s,p.x,p.y);if(p.y>cv.height+30){p.y=-30;p.x=rnd(0,cv.width)}}
   else{cx.save();cx.translate(p.x,p.y);cx.rotate(p.a);cx.fillStyle=p.c;cx.fillRect(-p.r/2,-p.r/4,p.r,p.r/2);cx.restore()}}
  parts=parts.filter(function(p){return p.s||p.y<cv.height+20})}
 requestAnimationFrame(frame)}
requestAnimationFrame(frame);
var reveal=function(){};
if(ANIM==='loading'){w.innerHTML='<div class="bar"><div id="b"></div></div><div id="pct">0%</div>';var v=0,b=document.getElementById('b'),pc=document.getElementById('pct');
 var t=setInterval(function(){v=Math.min(99,v+rnd(2,9));if(v>96)v=99;b.style.width=v+'%';pc.textContent=Math.floor(v)+'%'+(v>=99?' (almost…)':'')},600)}
else if(ANIM==='confetti'){burst(160);setInterval(function(){burst(40)},4000)}
else if(ANIM==='shake'){setInterval(function(){card.classList.remove('shake');void card.offsetWidth;card.classList.add('shake')},2500)}
else if(ANIM==='ticket'){var n='#'+Math.floor(rnd(100000,999999));w.innerHTML='<div class="ticket">TICKET '+n+'<br>SEAT: Z-404 &middot; ROW: Imaginary<br>VALID: Never</div>'}
else if(ANIM==='countdown'){w.innerHTML='<div id="count">10</div>';var c=10,el=document.getElementById('count');
 var ct=setInterval(function(){c--;el.textContent=c;if(c<=0){clearInterval(ct);el.textContent='0';doReveal()}},1000)}
else if(ANIM==='siren'){card.classList.add('siren')}
else if(ANIM==='matrix'){matrix()}
else if(ANIM==='float'){flakes(document.querySelector('.emoji').textContent.trim()||'👻',14)}
else if(ANIM==='snow'){flakes('❄',40)}
var AC=null;function tone(f,s,d,type){try{AC=AC||new (window.AudioContext||window.webkitAudioContext)();var o=AC.createOscillator(),g=AC.createGain();
 o.type=type||'sine';o.frequency.value=f;o.connect(g);g.connect(AC.destination);var t0=AC.currentTime+s;g.gain.setValueAtTime(.0001,t0);
 g.gain.exponentialRampToValueAtTime(.25,t0+.02);g.gain.exponentialRampToValueAtTime(.0001,t0+d);o.start(t0);o.stop(t0+d+.05)}catch(e){}}
var SOUNDS={chime:[[784,0,.3],[1047,.15,.45]],party:[[523,0,.15],[659,.12,.15],[784,.24,.15],[1047,.36,.4]],horn:[[220,0,.5,'sawtooth'],[175,.45,.6,'sawtooth']],
 drum:[[110,0,.15,'triangle'],[110,.2,.15,'triangle'],[165,.4,.3,'triangle']],arcade:[[880,0,.08,'square'],[660,.1,.08,'square'],[440,.2,.08,'square'],[220,.3,.3,'square']],
 siren:[[700,0,.35,'sawtooth'],[500,.35,.35,'sawtooth'],[700,.7,.35,'sawtooth']],beep:[[1200,0,.08,'square'],[1200,.15,.08,'square'],[1600,.3,.12,'square']],
 bell:[[1318,0,.6],[1318,.3,.6]],bells:[[1318,0,.25],[1175,.2,.25],[1047,.4,.25],[1568,.6,.5]],spooky:[[196,0,.8,'triangle'],[185,.5,.9,'triangle']],
 ufo:[[300,0,.2,'sine'],[600,.15,.2,'sine'],[900,.3,.2,'sine'],[1200,.45,.4,'sine']]};
function play(name){var s=SOUNDS[name];if(!s)return;s.forEach(function(n){tone(n[0],n[1],n[2],n[3])})}
var done=false;function doReveal(){if(done)return;done=true;document.getElementById('reveal').style.display='block';
 document.getElementById('go').textContent='😂 Got me!';if(mode!=='matrix')burst(200);if(SOUND)play('party')}
document.getElementById('go').addEventListener('click',function(){if(SOUND)play(SOUND);if(!done)setTimeout(doReveal,700)});
})();
</script>
</body>
</html>
""".trimIndent()
    }

    /** Shown for any address that isn't the prank page, and if a template goes missing. */
    fun fallback(): String = """
<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>PrankSpot</title></head><body style="font-family:system-ui;text-align:center;padding:40px 16px">
<h1>😄 PrankSpot</h1><p>The prank page is taking a nap.</p><p><a href="/">Try again</a></p></body></html>
""".trimIndent()

    fun html(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }

    /** A JS string literal that is also safe inside a <script> block. */
    fun js(s: String): String = buildString(s.length + 2) {
        append('"')
        for (c in s) when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '<' -> append("\\u003c")
            '>' -> append("\\u003e")
            '&' -> append("\\u0026")
            ' ' -> append("\\u2028")
            ' ' -> append("\\u2029")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }

    /** Only #RGB/#RRGGBB(AA) colours reach the stylesheet. */
    fun css(color: String): String =
        if (Regex("^#[0-9A-Fa-f]{3,8}$").matches(color)) color else "#888888"
}
