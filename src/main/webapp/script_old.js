var category = 'All';
var lastReload = 0;
var intervalId;

function ready(fn) {
    if (document.readyState != 'loading') {
        fn();
    } else if (document.addEventListener) {
        document.addEventListener('DOMContentLoaded', fn);
    } else {
        document.attachEvent('onreadystatechange', function() {
            if (document.readyState != 'loading')
                fn();
        });
    }
}

function addEventListener(el, eventName, eventHandler, selector) {
    if (selector) {
      const wrappedHandler = (e) => {
        if (e.target && e.target.matches(selector)) {
          eventHandler(e);
        }
      };
      el.addEventListener(eventName, wrappedHandler);
      return wrappedHandler;
    } else {
      el.addEventListener(eventName, eventHandler);
      return eventHandler;
    }
  }

async function getCategories() {
    const response = await fetch('api/soundFiles/categories');

    if (!response.ok) {
    }

    const body = await response.json();
    body.sort((a,b)=>{
        return (a < b) ? -1 : (a > b) ? 1 : 0;
    });
    var inHTML = '<li class="uk-nav-header">Kategorien</li>'
    inHTML+='<li kategorie="Alle" class="uk-active kategorie" ><a kategorie="Alle" href="#"><span class="uk-margin-small-right" uk-icon="icon:  chevron-right"></span> Alle</a></li>'
    inHTML+='<li kategorie="Neu" class="kategorie" ><a kategorie="Neu" href="#"><span class="uk-margin-small-right" uk-icon="icon:  chevron-right"></span> Neu</a></li>'
   
    inHTML+= '<li class="uk-nav-divider"></li>'

    body.forEach((kat, i) => {
        inHTML += ('<li  kategorie="' + kat + '" class="kategorie"><a kategorie="' + kat + '" href="#"><span class="uk-margin-small-right" uk-icon="icon:  chevron-right"></span> ' + kat + '</a></li>')
    });
    document.getElementById('kat_liste').innerHTML = '';
    document.getElementById('kat_liste').innerHTML += inHTML;
    document.getElementById('kat_liste_sidebar').innerHTML = '';
    document.getElementById('kat_liste_sidebar').innerHTML += inHTML;
}

async function getChannels(){
    const response = await fetch('/bot/channels');

    if (!response.ok) {
    }

    const body = await response.json();
    var inHTML = ''
    body.forEach((chan, i) => {
        if (chan != null){
            inHTML +='<option value="' + chan.id + '"' + ((chan.defaultChannel) ? 'selected' : '') + '>' + chan.name + '</option>'
        }       
    });
    document.getElementById('select_channel').innerHTML = '';
    document.getElementById('select_channel').innerHTML += inHTML;
}

async function getAllSounds() {
    const noti = UIkit.notification("<span class=\"uk-margin-small-right\" uk-spinner></span> Lade Sounds", {pos: 'top-right', timeout: 0})
    
    const response = await fetch('api/soundFiles/findAll');

    if (!response.ok) {
    }

    const body = await response.json();
    const sounds = body.content;

    const today = new Date();
    const  lastWeek = Date.parse(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 7));

    sounds.sort((a,b)=>{
        return (a.soundFileId.toUpperCase() < b.soundFileId.toUpperCase()) ? -1 : (a.soundFileId.toUpperCase() > b.soundFileId.toUpperCase()) ? 1 : 0;
    });
    inHTML = '';
    sounds.forEach((sound, i) => {
        var output = '<li times_played=';
        output += sound.timesPlayed + ' ';
        output += 'date_added=' + sound.dateAdded + ' ';
        output += 'id="' + sound.soundFileId + '" data-kategorie="' + sound.category + '" ';
        output += 'class=" oi-button uk-button uk-button-default uk-margin-small-left uk-margin-small-right uk-margin-small-bottom">';



        if (lastWeek < new Date(sound.dateAdded)){
            output += '<span class="uk-label uk-label-success">NEU</span> ';
        }

        output += sound.soundFileId + '</li>';
        inHTML += output;

    });
    UIkit.notification.closeAll()
    document.getElementById('sounds').innerHTML = '';
    document.getElementById('sounds').innerHTML += inHTML;
    
}

async function playSoundInChannel(soundId){
    selectedChannel = document.getElementById('select_channel').value;
    await fetch('/bot/playFileInChannel?soundFileId=' + soundId + '&voiceChannelId=' + selectedChannel, {
        method: 'POST'
      });
}

async function stop(){
    selectedChannel = document.getElementById('select_channel').value;
    await fetch('/bot/stop?voiceChannelId=' + selectedChannel, {
        method: 'POST'
      });
}

async function connect() {
    selectedChannel = document.getElementById('select_channel').value;
    await fetch('/bot/connect?voiceChannelId=' + selectedChannel,{
        method: 'POST'
    });
}
async function random(){
    selectedChannel = document.getElementById('select_channel').value;
    await fetch('/bot/random?voiceChannelId=' + selectedChannel, {
        method: 'POST'
      });
    
}

async function disconnect(){
    await fetch('/bot/disconnect', {
        method: 'POST'
      });   
}

async function reloadBot(){
    await fetch('/bot/reload', {
        method: 'POST'
      });  
    
}

async function playURL(url){
    selectedChannel = document.getElementById('select_channel').value;
    response = await fetch('/bot/playUrl?url=' + url + '&voiceChannelId=' + selectedChannel, {
        method: 'POST'
      });
    console.log(response);
      if (!response.ok) {
        const noti = UIkit.notification("<span class=\"uk-margin-small-right\" uk-icon=\"warning\"></span> Lade Sounds", {pos: 'top-right', timeout: 5, status: 'danger'})
    
      }
}

async function search(input) {
    var filter, card;
    filter = input.toUpperCase();
    card = document.getElementById("sounds");
    sounds = card.getElementsByTagName("li");
    [...sounds].forEach((sound, i) => {
        if(sound.id.toUpperCase().indexOf(filter) > -1){
            sound.style.opacity = 1;
            sound.style.display = "";
        } else {
            sound.style.opacity = 0;
            sound.style.display = "none"
        }

    
    });
}

async function filterCategory(cat) {
    console.log(cat);
    var filter, card;
    card = document.getElementById("sounds");
    sounds = card.getElementsByTagName("li");
    kategories = document.getElementsByClassName("kategorie");
    [...kategories].forEach((kategorie, i) => {
        if(kategorie.classList.contains("uk-active")){
            kategorie.classList.remove("uk-active");
        }
        if (kategorie.getAttribute("kategorie")==cat){
            kategorie.classList.add("uk-active");
        }
    });
    [...sounds].forEach((sound, i) => {
        if (cat === "Alle"){
            sound.style.opacity = 1;
            sound.style.display = "";
        }else if (cat === "Neu"){
            const today = new Date();
            const  lastWeek = Date.parse(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 7));
            if (lastWeek < new Date(sound.getAttribute('date_added'))){
                sound.style.opacity = 1;
                sound.style.display = "";
            }else{
                sound.style.opacity = 0;
            sound.style.display = "none"
            }
        }else if(sound.getAttribute('data-kategorie') == cat){
            sound.style.opacity = 1;
            sound.style.display = "";
        } else {
            sound.style.opacity = 0;
            sound.style.display = "none"
        }    
    });
}

async function showSoundInfoModal(soundID){
    sound = document.getElementById(soundID);
    inHTML = '<button class="uk-modal-close-default" type="button" uk-close></button>';
    inHTML += '<h2>' + sound.innerHTML +  '</h3>';
    inHTML += '<ul class="uk-nav uk-nav-default">';
    inHTML += '<li class="uk-nav-header">Kategorien</li>';
    inHTML += '<li>' +sound.getAttribute('data-kategorie') + '</li>';
    inHTML += '<li class="uk-nav-divider"></li>';
    inHTML += '<li class="uk-nav-header">Hinzugefügt am</li>';
    inHTML += '<li>' +(new Date(sound.getAttribute('date_added'))).toLocaleDateString() + '</li>';
    inHTML += '<li class="uk-nav-divider"></li>';
    inHTML += '<li class="uk-nav-header">Anzahl Wiedergaben seit letztem Reload</li>';
    inHTML += '<li>' + sound.getAttribute('times_played') + '</li>';  
    inHTML += '</ul>'
    document.getElementById('modal-info-inner').innerHTML = '';
    document.getElementById('modal-info-inner').innerHTML += inHTML;
    UIkit.modal(document.getElementById('modal-info')).show();
}

async function setupListeners(){

    document.addEventListener('contextmenu', (event) => {
        if (event.target.closest('#sounds li')) {
            event.preventDefault();
            showSoundInfoModal(event.target.id);
            return false;
        } else {
            return true;
        }
        
    }, false);

    document.addEventListener('click', (event) => {
        
        if (event.target.closest('li > span')) {
            playSoundInChannel(event.target.parentNode.id)
        }else if (event.target.closest('#sounds li')) {
            playSoundInChannel(event.target.id);
        }else if (event.target.id=='random') {
            random();
        }else if (event.target.id=='button_ALLE') {
            filterCategory("Alle");
        }else if (event.target.id=='stop') {
            stop();
        }else if (event.target.id=='disconnect') {
            disconnect();
        }else if(event.target.id == "play" || event.target.parentNode.id == "play"|| event.target.parentNode.parentNode.id == "play") {
            playURL(document.getElementById("url").value)
        }else if(event.target.id == "play-sidebar" || event.target.parentNode.id == "play-sidebar"|| event.target.parentNode.parentNode.id == "play-sidebar") {
            playURL(document.getElementById("url-sidebar").value)
        }else if (event.target.id == 'reload') {
            reloadBot();
        }else if (event.target.id == 'connect') {
            connect();
        }else if (event.target.id == 'settings') {
            UIkit.modal(document.getElementById('modal-settings')).show();
        } else if (event.target.closest('#kat_liste .kategorie')){
            document.getElementById("suche-sidebar").value="";
            document.getElementById("suche").value="";
            filterCategory(event.target.getAttribute('kategorie'));
            UIkit.offcanvas(document.getElementById('offcanvas')).hide();
        } else if (event.target.closest('#kat_liste_sidebar .kategorie')){
            filterCategory(event.target.getAttribute('kategorie'));
            UIkit.offcanvas(document.getElementById('offcanvas')).hide();
        } else if (event.target.id == 'suche-sidebar-a' || event.target.parentNode.id == 'suche-sidebar-a'|| event.target.parentNode.parentNode.id == 'suche-sidebar-a'){
            filterCategory("Alle");
            search(document.getElementById("suche-sidebar").value)
            UIkit.offcanvas(document.getElementById('offcanvas')).hide();
        } else if (event.target.id == 'suche-a' || event.target.parentNode.id == 'suche-a'|| event.target.parentNode.parentNode.id == 'suche-a'){
            filterCategory("Alle");
            search(document.getElementById("suche").value)
        }
      });
    document.getElementById('suche').addEventListener('keydown',(event)=>{
        filterCategory("Alle");
        search(document.getElementById("suche").value)
    });
    
}


async function reloadIssued(){
    UIkit.notification({
        message: "<span uk-icon='icon: refresh'></span> Sounds werden neu geladen!",
        status: 'primary',
        pos: 'top-center',
        timeout: 1000
    });

    UIkit.notification({
        message: "<span uk-icon='icon: refresh'></span> Sounds wurde neu geladen!",
        status: 'primary',
        pos: 'top-center',
        timeout: 1000
    });
}

async function setupReload(){
    const response = await fetch('bot/lastReload');

    if (!response.ok) {
    }

    const val = await response.text();
    lastReload = val;
    intervalId = window.setInterval(async ()=>{
        const response = await fetch('bot/lastReload');

        if (!response.ok) {
        }
    
        const val = await response.text();
        if (val != lastReload){
            console.log(val);
            console.log(lastReload);
            lastReload=val;
            reload();
            //clearInterval(intervalId)
            //UIkit.modal(document.getElementById('modal-reload')).show();
        }
    },5000);
}

async function reload(){
    getCategories();
    getAllSounds();
    getChannels();
}

window.ready(function() {
   
    reload();
    setupListeners();
    setupReload();
    
});

