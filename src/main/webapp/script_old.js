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
    var inHTML = ''
    body.forEach((kat, i) => {
        inHTML += ('<li  kategorie="' + kat + '" class="kategorie"><a kategorie="' + kat + '" href="#"><span class="uk-margin-small-right" uk-icon="icon:  chevron-right"></span> ' + kat + '</a></li>')
    });
    document.getElementById('kat_liste').innerHTML += inHTML;
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
    document.getElementById('select_channel').innerHTML += inHTML;
}

async function getAllSounds() {
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
        output += 'id="' + sound.soundFileId + '" data-kategorie="' + sound.category + '" ';
        output += 'class=" oi-button uk-button uk-button-default uk-margin-small-left uk-margin-small-right uk-margin-small-bottom">';



        //if (lastWeek < Date.parse(sound.dateAdded)){
        //    output += '<span class="uk-badge">NEU</span> ';
        //}

        output += sound.soundFileId + '</li>';
        inHTML += output;

    });
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

async function reload(){
    await fetch('/bot/reload', {
        method: 'POST'
      });  
    
}

async function search() {
    var filter, card;
    input = document.getElementById("suche").value;
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
    var filter, card;
    card = document.getElementById("sounds");
    sounds = card.getElementsByTagName("li");
    [...sounds].forEach((sound, i) => {
        if(sound.getAttribute('data-kategorie') == cat){
            sound.style.opacity = 1;
            sound.style.display = "";
        } else {
            sound.style.opacity = 0;
            sound.style.display = "none"
        }    
    });
}

async function setupListeners(){
    document.addEventListener('click', (event) => {
        if (event.target.closest('#sounds li')) {
            playSoundInChannel(event.target.id);
        }else if (event.target.id=='random') {
            random();
        }else if (event.target.id=='stop') {
            stop();
        }else if (event.target.id=='disconnect') {
            disconnect();
        }else if (event.target.id == 'reload') {
            reload();
        } else if (event.target.closest('#kat_liste .kategorie')){
            filterCategory(event.target.getAttribute('kategorie'));
            UIkit.offcanvas(document.getElementById('offcanvas')).hide();
        } else if (event.target.id == 'modal-reload-ok'){
            location.reload(); 
        }
      });
    document.getElementById('suche').addEventListener('keydown',(event)=>{
        search()
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
            clearInterval(intervalId)
            UIkit.modal(document.getElementById('modal-reload')).show();
        }
    },5000);
}

window.ready(function() {
    
    getCategories();
    getAllSounds();
    getChannels();
    setupListeners();
    setupReload();
    
});

