const labels={items:'物品',weapons:'武器',equipments:'裝備',gems:'寶石',affixes:'詞綴',rarities:'稀有度',sets:'套裝',classes:'職業',skills:'技能','skill-trees':'技能樹',quests:'任務',monsters:'怪物','drop-tables':'掉落表','crafting-stations':'製作站',npcs:'NPC','gui-layouts':'玩家 GUI','guild-settings':'工會設定'};
const defaults={
  items:{name:'未命名物品',base_item:'minecraft:paper',category:'material',rarity:'common',level:1,max_stack_size:64,lore:[],recipes:[],enabled:true},
  weapons:{name:'未命名武器',base_item:'minecraft:diamond_sword',rarity:'common',level:1,attack_damage:10,attack_speed:1.6,critical_chance:.05,tool_ability:'none',ability_unlock_level:1,ability_max_blocks:32,ability_radius:1,ability_cooldown_seconds:3,abilities:[],affix_pool:[],enabled:true},
  equipments:{name:'未命名裝備',base_item:'minecraft:iron_chestplate',equipment_slot:'chest',rarity:'common',level:1,armor:6,armor_toughness:0,knockback_resistance:0,health_bonus:0,abilities:[],affix_pool:[],enabled:true},
  gems:{name:'未命名寶石',material:'minecraft:amethyst_shard',stats:[{stat:'health',value:10}],enabled:true},
  affixes:{name:'未命名詞綴',stat:'damage',min:1,max:5,weight:10,enabled:true},
  rarities:{name:'普通',color:'<white>',weight:100,enabled:true},
  sets:{name:'未命名套裝',bonuses:[],enabled:true},classes:{name:'未命名職業',base_stats:{},skills:[],enabled:true},
  skills:{name:'未命名技能',trigger:'manual',mana_cost:0,cooldown_seconds:1,effects:[{type:'damage',value:1,level_scale:0,intelligence_scale:0,strength_scale:0,skill_level_scale:0,target:'target'}],enabled:true},
  'skill-trees':{name:'未命名技能樹',class_id:'',nodes:[],enabled:true},
  quests:{name:'未命名任務',type:'side',description:[],npc:'',prerequisite:'',objectives:[],rewards:{},enabled:true},
  monsters:{name:'未命名怪物',entity_type:'minecraft:zombie',level:1,max_health:20,damage:2,movement_speed:.23,follow_range:32,armor:0,knockback_resistance:0,equipment:[],drops:[],enabled:true},
  'drop-tables':{name:'未命名掉落表',rolls:1,entries:[],enabled:true},
  'crafting-stations':{name:'未命名製作站',title:'製作站',recipes:[],enabled:true},
  npcs:{name:'未命名 NPC',world:'world',x:0,y:64,z:0,yaw:0,pitch:0,dialogue:[],enabled:true},
  'gui-layouts':{name:'未命名玩家介面',title:'介面標題',rows:6,buttons:[],enabled:true},
  'guild-settings':{name:'工會系統設定',creation_cost:0,max_members:50,invite_expiry_minutes:5,donation_xp_ratio:.1,enabled:true}
};
const state={token:sessionStorage.getItem('crest-rpg-token')||'',kind:'gems',drafts:{},key:null,counts:{},mode:'create',editing:null,assets:[],minecraftIcons:[],skillOptions:[],rarityOptions:[],contentOptions:[],materialPath:null,materialApply:null,materialCategory:'all',materialLimit:80};
const $=id=>document.getElementById(id);
const fieldLabels={key:'key（內容代碼）',name:'name（顯示名稱）',base_item:'base_item（Minecraft 基礎材質）',material:'material（Minecraft 材質）',texture:'texture（自訂貼圖素材）',model:'model（自訂模型素材）',custom_model_data:'Custom Model Data（連結資源包外觀）',lore:'lore（物品說明文字）',rarity:'rarity（稀有度）',level:'level（需求或內容等級）',enabled:'enabled（是否啟用）',category:'category（內容分類）',max_stack_size:'max_stack_size（堆疊上限）',attack_damage:'attack_damage（攻擊傷害）',attack_speed:'attack_speed（攻擊速度）',critical_chance:'critical_chance（暴擊機率）',affix_pool:'affix_pool（可抽取的詞綴池）',equipment_slot:'equipment_slot（裝備部位）',armor:'armor（護甲值）',armor_toughness:'armor_toughness（護甲韌性）',health_bonus:'health_bonus（額外生命值）',stat:'stat（影響的屬性）',value:'value（效果數值）',min:'min（最小數量或數值）',max:'max（最大數量或數值）',weight:'weight（抽選權重）',color:'color（顯示顏色）',stats:'stats（屬性加成）',requirements:'requirements（使用需求）',bonuses:'bonuses（套裝效果）',skills:'skills（可用技能）',base_stats:'base_stats（職業基礎屬性）',trigger:'trigger（效果觸發方式）',mana_cost:'mana_cost（施放所需法力）',cooldown_seconds:'cooldown_seconds（冷卻秒數）',effects:'effects（技能效果清單）',class_id:'class_id（所屬職業代碼）',nodes:'nodes（技能樹節點）',type:'type（內容或效果類型）',description:'description（顯示說明）',npc:'npc（關聯 NPC）',prerequisite:'prerequisite（前置任務）',objectives:'objectives（任務目標）',rewards:'rewards（完成獎勵）',entity_type:'entity_type（Minecraft 怪物類型）',max_health:'max_health（最大生命值）',damage:'damage（攻擊傷害）',movement_speed:'movement_speed（移動速度）',follow_range:'follow_range（追蹤玩家距離）',knockback_resistance:'knockback_resistance（擊退抗性）',equipment:'equipment（怪物裝備與出現機率）',drops:'drops（怪物掉落物）',chance:'chance（觸發或掉落機率）',slot:'slot（裝備或配方位置）',item_key:'item_key（原版或 RPG 物品代碼）',rolls:'rolls（掉落抽選次數）',entries:'entries（掉落項目）',title:'title（介面標題）',recipes:'recipes（製作配方）',world:'world（生成世界名稱）',x:'X（東西方向座標）',y:'Y（高度座標）',z:'Z（南北方向座標）',yaw:'Yaw（左右朝向角度）',pitch:'Pitch（上下視角角度）',dialogue:'dialogue（NPC 對話內容）',tool_ability:'tool_ability（工具特殊能力）',ability_unlock_level:'ability_unlock_level（能力解鎖等級）',ability_max_blocks:'ability_max_blocks（能力最多影響方塊數）',ability_radius:'ability_radius（能力影響半徑）',ability_cooldown_seconds:'ability_cooldown_seconds（能力冷卻秒數）',abilities:'abilities（隨機或綁定能力）',effect:'effect（狀態效果）',min_level:'min_level（最低生效等級）',max_level:'max_level（最高生效等級）',base_value:'base_value（基礎效果強度）',value_per_level:'value_per_level（每級增加強度）',output:'output（製作產物）',amount:'amount（物品數量）',ingredients:'ingredients（3×3 配方材料）',required_level:'required_level（製作需求等級）',permission:'permission（製作所需權限）'};
Object.assign(fieldLabels,{target:'target（效果作用目標）',level_scale:'level_scale（角色每級增加數值）',intelligence_scale:'intelligence_scale（每點智力增加數值）',strength_scale:'strength_scale（每點力量增加數值）',skill_level_scale:'skill_level_scale（技能每級增加數值）',missing_health_scale:'missing_health_scale（依已損失生命增加數值）',potion:'potion（藥水狀態效果）',seconds:'seconds（持續秒數）',amplifier:'amplifier（效果等級，0 代表一級）',sound:'sound（播放的 Minecraft 音效）',volume:'volume（音效音量）',pitch:'pitch（音效音高或 NPC 上下視角）',particle:'particle（顯示的粒子類型）',count:'count（粒子或目標數量）',power:'power（推力或衝刺強度）',vertical:'vertical（向上推力）',radius:'radius（作用半徑）',range:'range（最遠作用距離）',targets:'targets（最多連鎖目標數）',speed:'speed（投射物飛行速度）',pulses:'pulses（重複生效次數）',interval_seconds:'interval_seconds（每次效果間隔秒數）',ratio:'ratio（吸血比例，0.5 代表 50%）',entity:'entity（召喚的生物類型）'});
Object.assign(fieldLabels,{rows:'rows（介面列數，1 至 6）',buttons:'buttons（介面按鈕與格位）',action:'action（玩家點擊後執行功能）'});
Object.assign(fieldLabels,{creation_cost:'creation_cost（建立工會需要的金額）',max_members:'max_members（每個工會成員上限）',invite_expiry_minutes:'invite_expiry_minutes（邀請有效分鐘數）',donation_xp_ratio:'donation_xp_ratio（每元捐獻轉換的工會經驗）'});
const arrayTemplates={lore:'',affix_pool:'',skills:'',description:'',dialogue:'',bonuses:{pieces:2,stats:{}},effects:{type:'damage',value:1},nodes:{key:'node_1',skill:'',requires:[]},objectives:{type:'kill',target:'minecraft:zombie',count:1,display:'擊殺殭屍'},entries:{key:'minecraft:rotten_flesh',weight:10,min:1,max:1},drops:{item_key:'minecraft:rotten_flesh',chance:.25,min:1,max:1},equipment:{slot:'main_hand',item_key:'minecraft:iron_sword',chance:1},abilities:{type:'potion',trigger:'passive',chance:1,min_level:1,max_level:100,effect:'speed',base_value:1,value_per_level:0,skill:''},recipes:{key:'recipe_1',output:'minecraft:stone',amount:1,ingredients:[]},ingredients:{key:'minecraft:cobblestone',amount:1}};
const skillEffectTypes={damage:'造成傷害',heal:'回復生命',mana:'回復法力',message:'顯示訊息',potion:'藥水效果',sound:'播放音效',particle:'產生粒子',knockback:'擊退目標',fire:'點燃目標',area:'範圍效果',beam:'光束攻擊',chain:'連鎖效果',projectile:'發射投射物',dash:'向前衝刺',teleport:'瞬間移動',shield:'生命護盾',damage_over_time:'持續傷害',heal_over_time:'持續治療',lifesteal:'生命偷取',summon:'召喚生物',sequence:'依序執行效果',command:'執行主機指令'};
const statChoices={health:'最大生命值',damage:'傷害',armor:'護甲',armor_toughness:'護甲韌性',strength:'力量',intelligence:'智力',agility:'敏捷',critical_chance:'暴擊機率',critical_damage:'暴擊傷害',attack_speed:'攻擊速度',movement_speed:'移動速度',mana:'最大法力',mana_regeneration:'法力恢復',magic_damage:'魔法傷害',knockback_resistance:'擊退抗性'};
const skillEffectTemplates={
  damage:{type:'damage',value:10,level_scale:0,intelligence_scale:0,strength_scale:0,skill_level_scale:0,target:'target'},
  heal:{type:'heal',value:10,level_scale:0,intelligence_scale:0,skill_level_scale:0,target:'self'},mana:{type:'mana',value:10},
  message:{type:'message',value:'技能發動！'},command:{type:'command',value:'say {player} 使用了技能'},
  potion:{type:'potion',potion:'speed',seconds:5,amplifier:0,target:'self'},sound:{type:'sound',sound:'minecraft:entity.experience_orb.pickup',volume:1,pitch:1},
  particle:{type:'particle',particle:'CRIT',count:10,target:'target'},knockback:{type:'knockback',power:1,vertical:.3,target:'target'},fire:{type:'fire',seconds:3,target:'target'},
  area:{type:'area',radius:4,effects:[{type:'damage',value:5}]},beam:{type:'beam',range:20,effects:[{type:'damage',value:8}]},chain:{type:'chain',targets:3,radius:6,effects:[{type:'damage',value:5}]},
  projectile:{type:'projectile',speed:1.5,material:'minecraft:snowball',effects:[{type:'damage',value:8}]},dash:{type:'dash',power:1.5,vertical:.15},teleport:{type:'teleport',range:8},
  shield:{type:'shield',value:10,seconds:5,level_scale:0,intelligence_scale:0},damage_over_time:{type:'damage_over_time',value:2,pulses:5,interval_seconds:1,target:'target'},
  heal_over_time:{type:'heal_over_time',value:2,pulses:5,interval_seconds:1,target:'self'},lifesteal:{type:'lifesteal',value:8,ratio:.5},
  summon:{type:'summon',entity:'WOLF',amount:1,seconds:20},sequence:{type:'sequence',interval_seconds:.25,effects:[{type:'damage',value:5}]}
};

async function api(path,options={}){
  const response=await fetch(path,{...options,headers:{'Authorization':`Bearer ${state.token}`,'Content-Type':'application/json',...(options.headers||{})}});
  let payload={};try{payload=await response.json()}catch{}
  if(!response.ok){const error=new Error(payload.error||`請求失敗 (${response.status})`);error.status=response.status;error.payload=payload;throw error}
  return payload;
}

async function login(token){
  state.token=token.trim();
  const session=await api('/api/session');
  sessionStorage.setItem('crest-rpg-token',state.token);
  $('version').textContent=`CrestRPG v${session.version}`;
  $('login').classList.add('hidden');$('app').classList.remove('hidden');
  await refreshKinds();await loadAssets(false);await loadKind(state.kind);
}

async function refreshKinds(){
  const data=await api('/api/kinds');state.counts=data.counts;
  $('kind-nav').replaceChildren(...data.kinds.map(kind=>{
    const button=document.createElement('button');button.className=`nav-item${kind===state.kind?' active':''}`;button.dataset.kind=kind;
    const label=document.createElement('span');label.textContent=labels[kind]||kind;
    const count=document.createElement('span');count.className='nav-count';count.textContent=data.counts[kind]||0;
    button.append(label,count);button.addEventListener('click',()=>loadKind(kind));return button;
  }));
}

async function loadKind(kind){
  state.kind=kind;state.key=null;
  const rarities=await api('/api/drafts?kind=rarities');state.rarityOptions=Object.entries(rarities.drafts||{}).map(([key,value])=>({key,name:value.name||key,enabled:value.enabled!==false}));
  if(['classes','weapons','equipments'].includes(kind)){const skills=await api('/api/drafts?kind=skills');state.skillOptions=Object.entries(skills.drafts||{}).map(([key,value])=>({key,name:value.name||key}))}
  if(kind==='monsters'){const groups=await Promise.all(['items','weapons','equipments'].map(value=>api(`/api/drafts?kind=${value}`)));state.contentOptions=groups.flatMap(group=>Object.entries(group.drafts||{}).map(([key,value])=>({key,name:value.name||key})))}
  const data=await api(`/api/drafts?kind=${encodeURIComponent(kind)}`);state.drafts=data.drafts||{};
  $('page-title').textContent=`${labels[kind]||kind}編輯器`;
  document.querySelectorAll('.nav-item').forEach(node=>node.classList.toggle('active',node.dataset.kind===kind));
  $('editor-form').classList.add('hidden');$('editor-placeholder').classList.remove('hidden');
  renderList();
}

function renderList(){
  const query=$('search').value.trim().toLowerCase();
  const entries=Object.entries(state.drafts).filter(([key,value])=>key.includes(query)||String(value.name||'').toLowerCase().includes(query));
  $('draft-count').textContent=`${entries.length} 筆`;$('empty').classList.toggle('hidden',entries.length!==0);
  $('draft-list').replaceChildren(...entries.map(([key,value])=>{
    const button=document.createElement('button');button.className=`draft-row${state.key===key?' active':''}`;
    const text=document.createElement('span');const strong=document.createElement('strong');strong.textContent=value.name||key;
    const code=document.createElement('code');code.textContent=key;text.append(strong,code);
    const dot=document.createElement('span');dot.className=`status-dot${value.enabled===false?'':' enabled'}`;dot.title=value.enabled===false?'已停用':'啟用中';
    button.append(text,dot);button.addEventListener('click',()=>openDraft(key));return button;
  }));
}

function openDraft(key){
  state.key=key;state.editing=structuredClone(state.drafts[key]);if(['items','weapons','equipments','gems'].includes(state.kind)){state.editing.texture??='';state.editing.model??=''}if(state.kind==='gems'){if(!Array.isArray(state.editing.stats))state.editing.stats=[{stat:state.editing.stat||'health',value:Number(state.editing.value)||0}];delete state.editing.stat;delete state.editing.value}if(state.kind==='items')state.editing.recipes??=[];if(state.kind==='weapons'){state.editing.tool_ability??='none';state.editing.ability_unlock_level??=state.editing.level||1;state.editing.ability_max_blocks??=32;state.editing.ability_radius??=1;state.editing.ability_cooldown_seconds??=3;state.editing.abilities??=[]}if(state.kind==='equipments')state.editing.abilities??=[];$('draft-heading').textContent=key;$('draft-json').value=JSON.stringify(state.editing,null,2);
  $('editor-placeholder').classList.add('hidden');$('editor-form').classList.remove('hidden');$('save-status').textContent='';
  $('advanced-json').open=false;renderVisualFields();checkJson();renderList();
}

function renderVisualFields(){const root=$('visual-fields');root.replaceChildren();renderObject(state.editing,root,[])}
function renderObject(object,container,path){
  for(const [key,value] of Object.entries(object)){
    if(key==='key')continue;
    const next=[...path,key];
    if(Array.isArray(value)){renderArray(value,container,next,key);continue}
    if(value&&typeof value==='object'){
      const fieldset=document.createElement('fieldset');fieldset.className='object-field';const legend=document.createElement('legend');legend.textContent=fieldLabels[key]||humanize(key);fieldset.append(legend);renderObject(value,fieldset,next);container.append(fieldset);continue
    }
    const wrapper=document.createElement('div');wrapper.className=`form-field${typeof value==='string'&&value.length>80?' full':''}`;
    const label=document.createElement('label');label.textContent=fieldLabels[key]||humanize(key);wrapper.append(label);
    if(key==='rarity'){
      const select=document.createElement('select');select.className='field-select';
      const options=[...state.rarityOptions];
      if(value&&!options.some(option=>option.key===value))options.unshift({key:value,name:`目前使用：${value}`,enabled:false,missing:true});
      if(!options.length){const option=document.createElement('option');option.value=value||'common';option.textContent='尚未建立稀有度（請先到稀有度編輯器新增）';select.append(option)}
      else for(const rarity of options){const option=document.createElement('option');option.value=rarity.key;option.textContent=`${rarity.name}（${rarity.key}${rarity.missing?'，找不到設定':rarity.enabled?'':'，已停用'}）`;select.append(option)}
      select.value=value||options[0]?.key||'common';select.addEventListener('change',()=>{setPath(state.editing,next,select.value);syncJson()});wrapper.append(select);container.append(wrapper);continue
    }
    if(key==='equipment_slot'){
      const select=choiceSelect({head:'頭部（頭盔）',chest:'身體（胸甲）',legs:'腿部（護腿）',feet:'腳部（靴子）',off_hand:'副手（盾牌或副手裝備）'},value,next);wrapper.append(select);container.append(wrapper);continue
    }
    if(key==='slot'&&path.includes('equipment')){
      const select=choiceSelect({main_hand:'主手（怪物持有的武器）',off_hand:'副手（盾牌或副手物品）',head:'頭部（頭盔）',chest:'身體（胸甲）',legs:'腿部（護腿）',feet:'腳部（靴子）'},value,next);wrapper.append(select);container.append(wrapper);continue
    }
    if(state.kind==='skills'&&key==='type'&&path.includes('effects')){const select=document.createElement('select');select.className='field-select';for(const [effectType,effectLabel] of Object.entries(skillEffectTypes)){const option=document.createElement('option');option.value=effectType;option.textContent=`${effectLabel} (${effectType})`;select.append(option)}if(!skillEffectTypes[value]){const option=document.createElement('option');option.value=value;option.textContent=`自訂：${value}`;select.append(option)}select.value=value;select.addEventListener('change',()=>{setPath(state.editing,path,structuredClone(skillEffectTemplates[select.value]||{type:select.value}));syncJson();renderVisualFields()});wrapper.append(select);container.append(wrapper);continue}
    if(key==='tool_ability'){const select=document.createElement('select');select.className='field-select';for(const [ability,labelText] of Object.entries({none:'無',vein_mining:'連鎖挖礦',tree_felling:'整棵砍伐',right_click_harvest:'右鍵收成並重種'})){const option=document.createElement('option');option.value=ability;option.textContent=labelText;select.append(option)}select.value=value||'none';select.addEventListener('change',()=>{setPath(state.editing,next,select.value==='none'?'':select.value);syncJson()});wrapper.append(select);container.append(wrapper);continue}
    if(key==='skill'&&path.includes('abilities')){const select=document.createElement('select');select.className='field-select';const none=document.createElement('option');none.value='';none.textContent='不綁定技能';select.append(none);for(const skill of state.skillOptions){const option=document.createElement('option');option.value=skill.key;option.textContent=`${skill.name} (${skill.key})`;select.append(option)}select.value=value||'';select.addEventListener('change',()=>{setPath(state.editing,next,select.value);syncJson()});wrapper.append(select);container.append(wrapper);continue}
    if(path.includes('abilities')&&key==='type'){const select=choiceSelect({potion:'被動狀態效果',skill:'綁定技能'},value,next);wrapper.append(select);container.append(wrapper);continue}
    if(path.includes('abilities')&&key==='trigger'){const select=choiceSelect({passive:'穿戴時持續生效',right_click:'右鍵觸發',shift_right_click:'潛行右鍵觸發'},value,next);wrapper.append(select);container.append(wrapper);continue}
    if(path.includes('abilities')&&key==='effect'){const select=choiceSelect({speed:'速度提升',water_breathing:'水下呼吸',night_vision:'夜視',jump_boost:'跳躍提升',haste:'挖掘加速',strength:'力量',resistance:'抗性',fire_resistance:'火焰抗性',regeneration:'生命恢復',dolphins_grace:'海豚恩惠'},value,next);wrapper.append(select);container.append(wrapper);continue}
    if(state.kind==='skills'&&path.includes('effects')&&key==='target'){const select=choiceSelect({target:'目前目標',self:'施法玩家自己'},value,next);wrapper.append(select);container.append(wrapper);continue}
    if(state.kind==='skills'&&path.includes('effects')&&key==='potion'){const select=choiceSelect({speed:'速度提升',slowness:'緩慢',haste:'挖掘加速',mining_fatigue:'挖掘疲勞',strength:'力量',instant_health:'瞬間治療',instant_damage:'瞬間傷害',jump_boost:'跳躍提升',regeneration:'生命恢復',resistance:'抗性',fire_resistance:'火焰抗性',water_breathing:'水下呼吸',invisibility:'隱形',night_vision:'夜視',weakness:'虛弱',poison:'中毒',wither:'凋零',absorption:'額外生命',saturation:'飽食',glowing:'發光',levitation:'漂浮',slow_falling:'緩降',conduit_power:'海靈祝福',dolphins_grace:'海豚恩惠'},value,next);wrapper.append(select);container.append(wrapper);continue}
    if(state.kind==='gui-layouts'&&key==='action'){const select=choiceSelect({none:'純裝飾，不執行動作',profile:'顯示玩家資料',classes:'開啟轉職選單',stats:'開啟技能與屬性',content:'開啟可用內容',skill_bar:'開啟技能快捷列',skill_tree:'開啟技能樹',crafting:'開啟製作站',commands:'開啟指令選單',quests:'開啟任務介面',party:'開啟隊伍介面',guild:'開啟工會介面',back:'返回上一頁',close:'關閉介面'},value,next);wrapper.append(select);container.append(wrapper);continue}
    const input=document.createElement('input');
    if(typeof value==='boolean'){input.type='checkbox';input.checked=value;input.addEventListener('change',()=>{setPath(state.editing,next,input.checked);syncJson()})}
    else if(typeof value==='number'){input.type='number';input.step='any';input.value=value;input.addEventListener('input',()=>{setPath(state.editing,next,input.value===''?0:Number(input.value));syncJson()})}
    else{input.type='text';input.value=value??'';if(key==='texture'||key==='model'){const options=document.createElement('datalist');options.id=`asset-${Math.random().toString(36).slice(2)}`;const type=key==='texture'?'texture':'model';for(const asset of state.assets.filter(a=>a.type===type)){const option=document.createElement('option');option.value=asset.name;options.append(option)}input.setAttribute('list',options.id);wrapper.append(options)}if(key==='item_key'&&state.kind==='monsters'){const options=document.createElement('datalist');options.id=`content-${Math.random().toString(36).slice(2)}`;for(const content of state.contentOptions){const option=document.createElement('option');option.value=content.key;option.label=content.name;options.append(option)}input.setAttribute('list',options.id);wrapper.append(options)}input.addEventListener('input',()=>{setPath(state.editing,next,input.value);syncJson()})}
    wrapper.append(input);if(key==='base_item'||key==='material'){wrapper.classList.add('material-field');const preview=document.createElement('img');preview.className='inline-material-icon';loadMinecraftIcon(preview,value);wrapper.insertBefore(preview,input);const pick=document.createElement('button');pick.type='button';pick.className='button secondary material-pick';pick.textContent='從 Minecraft 圖示選擇';pick.addEventListener('click',()=>openMaterialPicker(next));wrapper.append(pick)}if(key==='item_key'&&state.kind==='monsters'){const pick=document.createElement('button');pick.type='button';pick.className='button secondary material-pick';pick.textContent='選擇原版物品';pick.addEventListener('click',()=>openMaterialPicker(next));wrapper.append(pick)}if(key==='entity_type'){const pick=document.createElement('button');pick.type='button';pick.className='button secondary material-pick';pick.textContent='圖形化選擇怪物類型';pick.addEventListener('click',()=>openEntityPicker(next));wrapper.append(pick)}if(key==='texture'||key==='model'){const upload=document.createElement('input');upload.type='file';upload.className='inline-upload';upload.accept=key==='texture'?'image/png':'application/json,.json';upload.addEventListener('change',async()=>{const result=await uploadAsset(upload.files[0],key==='texture'?'texture':'model');if(result){input.value=result.name;setPath(state.editing,next,result.name);syncJson();renderVisualFields()}});wrapper.append(upload)}container.append(wrapper);
  }
}
function renderArray(array,container,path,key){
  if(state.kind==='gui-layouts'&&key==='buttons'){renderGuiButtons(array,container,path);return}
  if(state.kind==='gems'&&key==='stats'){renderGemStats(array,container,path);return}
  if(state.kind==='classes'&&key==='skills'){renderSkillChecklist(array,container,path);return}
  if((state.kind==='crafting-stations'||state.kind==='items')&&key==='recipes'){renderCraftingRecipes(array,container,path);return}
  const section=document.createElement('section');section.className='array-field';const heading=document.createElement('div');heading.className='array-heading';const title=document.createElement('span');title.textContent=fieldLabels[key]||humanize(key);
  const add=document.createElement('button');add.type='button';add.className='button secondary';add.textContent='＋ 新增';add.addEventListener('click',()=>{array.push(structuredClone(arrayTemplates[key]??(array.length?templateFrom(array[0]):'')));syncJson();renderVisualFields()});heading.append(title,add);section.append(heading);
  if(array.length===0){const empty=document.createElement('p');empty.className='field-help';empty.textContent='目前沒有項目。';section.append(empty)}
  array.forEach((value,index)=>{const item=document.createElement('div');item.className='array-item';const remove=document.createElement('button');remove.type='button';remove.className='array-remove';remove.textContent='刪除';remove.addEventListener('click',()=>{array.splice(index,1);syncJson();renderVisualFields()});item.append(remove);
    if(value&&typeof value==='object'&&!Array.isArray(value))renderObject(value,item,[...path,index]);else{const input=document.createElement('input');input.type=typeof value==='number'?'number':'text';input.value=value??'';input.addEventListener('input',()=>{array[index]=typeof value==='number'?Number(input.value):input.value;syncJson()});item.append(input)}section.append(item)});container.append(section)
}
function renderGemStats(stats,container,path){const section=document.createElement('section');section.className='array-field';const heading=document.createElement('div');heading.className='array-heading';const title=document.createElement('span');title.textContent='stats（鑲嵌後增加的屬性清單）';const add=document.createElement('button');add.type='button';add.className='button secondary';add.textContent='＋ 新增屬性';add.addEventListener('click',()=>{stats.push({stat:'health',value:1});syncJson();renderVisualFields()});heading.append(title,add);section.append(heading);if(!stats.length){const empty=document.createElement('p');empty.className='field-help';empty.textContent='尚未設定屬性，這顆寶石鑲嵌後不會提供加成。';section.append(empty)}stats.forEach((entry,index)=>{const row=document.createElement('div');row.className='array-item';const label=document.createElement('label');label.textContent='stat（影響的屬性）';const select=choiceSelect(statChoices,entry.stat||'health',[...path,index,'stat']);label.append(select);const valueLabel=document.createElement('label');valueLabel.textContent='value（此屬性的加成數值）';const input=document.createElement('input');input.type='number';input.step='any';input.value=Number(entry.value)||0;input.addEventListener('input',()=>{entry.value=Number(input.value)||0;syncJson()});valueLabel.append(input);const remove=document.createElement('button');remove.type='button';remove.className='array-remove';remove.textContent='刪除';remove.addEventListener('click',()=>{stats.splice(index,1);syncJson();renderVisualFields()});row.append(remove,label,valueLabel);section.append(row)});container.append(section)}
function renderGuiButtons(buttons,container,path){const rows=Math.max(1,Math.min(6,Number(state.editing.rows)||6));const section=document.createElement('section');section.className='array-field gui-layout-editor';const heading=document.createElement('div');heading.className='array-heading';const title=document.createElement('span');title.textContent='🧰 遊戲內箱子 GUI 預覽';const add=document.createElement('button');add.type='button';add.className='button secondary';add.textContent='＋ 新增按鈕';add.addEventListener('click',()=>{const used=new Set(buttons.map(button=>Number(button.slot)));let slot=0;while(used.has(slot)&&slot<rows*9)slot++;if(slot>=rows*9){toast('目前介面已沒有空格');return}buttons.push({slot,material:'minecraft:stone_button',name:'新按鈕',lore:['點擊執行功能'],action:'none',enabled:true});syncJson();renderVisualFields()});heading.append(title,add);section.append(heading);const help=document.createElement('p');help.className='field-help';help.textContent='點擊空格新增按鈕；按鈕的 slot 是從 0 開始的箱子格位。可使用 {player}、{level}、{class} 顯示玩家資料。';section.append(help);const grid=document.createElement('div');grid.className='gui-grid';grid.style.gridTemplateRows=`repeat(${rows},64px)`;for(let slot=0;slot<rows*9;slot++){const entry=buttons.find(button=>Number(button.slot)===slot);const cell=document.createElement('button');cell.type='button';cell.className=`gui-cell${entry?' filled':''}`;cell.title=entry?`${entry.name} · 格位 ${slot}`:`空格位 ${slot}`;if(entry){const img=document.createElement('img');loadMinecraftIcon(img,entry.material);const name=document.createElement('small');name.textContent=entry.name||entry.action||'按鈕';cell.append(img,name)}else cell.textContent=String(slot);cell.addEventListener('click',()=>{if(entry){document.getElementById(`gui-button-${slot}`)?.scrollIntoView({behavior:'smooth',block:'center'});return}buttons.push({slot,material:'minecraft:stone_button',name:'新按鈕',lore:['點擊執行功能'],action:'none',enabled:true});syncJson();renderVisualFields()});grid.append(cell)}section.append(grid);buttons.sort((a,b)=>Number(a.slot)-Number(b.slot));buttons.forEach((button,index)=>{const card=document.createElement('fieldset');card.id=`gui-button-${button.slot}`;card.className='object-field gui-button-card';const legend=document.createElement('legend');legend.textContent=`格位 ${button.slot} · ${button.name||'未命名按鈕'}`;const remove=document.createElement('button');remove.type='button';remove.className='array-remove';remove.textContent='刪除按鈕';remove.addEventListener('click',()=>{buttons.splice(index,1);syncJson();renderVisualFields()});card.append(legend,remove);renderObject(button,card,[...path,index]);section.append(card)});container.append(section)}
function renderSkillChecklist(skills,container,path){const section=document.createElement('section');section.className='array-field';const heading=document.createElement('div');heading.className='array-heading';heading.textContent='可使用技能（從已建立技能勾選）';section.append(heading);const grid=document.createElement('div');grid.className='check-grid';for(const option of state.skillOptions){const label=document.createElement('label');const input=document.createElement('input');input.type='checkbox';input.checked=skills.includes(option.key);input.addEventListener('change',()=>{if(input.checked&&!skills.includes(option.key))skills.push(option.key);else if(!input.checked)skills.splice(skills.indexOf(option.key),1);syncJson()});label.append(input,document.createTextNode(`${option.name} (${option.key})`));grid.append(label)}if(!state.skillOptions.length)grid.textContent='請先在「技能」編輯器建立技能。';section.append(grid);container.append(section)}
function renderCraftingRecipes(recipes,container,path){
  const itemRecipe=state.kind==='items';
  const section=document.createElement('section');section.className='array-field crafting-recipes';
  const heading=document.createElement('div');heading.className='array-heading';const title=document.createElement('span');title.textContent=itemRecipe?'🧰 此物品的自訂合成配方':'🧰 製作配方';
  const add=document.createElement('button');add.type='button';add.className='button secondary';add.textContent='＋ 新增配方';add.addEventListener('click',()=>{recipes.push({key:`${itemRecipe?state.key+'_':''}recipe_${recipes.length+1}`,output:itemRecipe?state.key:'minecraft:stone',amount:1,required_level:1,permission:'',ingredients:[]});syncJson();renderVisualFields()});heading.append(title,add);section.append(heading);
  if(itemRecipe){const help=document.createElement('p');help.className='field-help';help.textContent='這裡建立的配方會自動放進「物品合成」製作站，遊戲內使用 /rpgcraft item_crafting 開啟。';section.append(help)}
  if(!recipes.length){const empty=document.createElement('p');empty.className='field-help';empty.textContent='尚無配方，點「新增配方」開始排列 3×3 材料。';section.append(empty)}
  recipes.forEach((recipe,recipeIndex)=>{
    if(itemRecipe)recipe.output=state.key;
    recipe.ingredients=Array.isArray(recipe.ingredients)?recipe.ingredients:[];
    recipe.ingredients.forEach((ingredient,index)=>{if(ingredient&&ingredient.slot===undefined)ingredient.slot=index});
    const card=document.createElement('article');card.className='crafting-card';
    const top=document.createElement('div');top.className='crafting-card-head';const name=document.createElement('strong');name.textContent=`配方 ${recipeIndex+1}`;const remove=document.createElement('button');remove.type='button';remove.className='array-remove';remove.textContent='刪除配方';remove.addEventListener('click',()=>{recipes.splice(recipeIndex,1);syncJson();renderVisualFields()});top.append(name,remove);card.append(top);
    const settings=document.createElement('div');settings.className='crafting-settings';
    settings.append(craftingInput('配方代碼',recipe.key||'',value=>recipe.key=value),craftingInput('需求等級',recipe.required_level??1,value=>recipe.required_level=Math.max(1,Number(value)||1),'number'),craftingInput('權限（可留空）',recipe.permission||'',value=>recipe.permission=value));card.append(settings);
    const bench=document.createElement('div');bench.className='crafting-bench';const grid=document.createElement('div');grid.className='crafting-grid';
    for(let slot=0;slot<9;slot++){const ingredient=recipe.ingredients.find(item=>item&&Number(item.slot)===slot);const cell=document.createElement('div');cell.className=`crafting-slot${ingredient?' filled':''}`;const choose=document.createElement('button');choose.type='button';choose.title=ingredient?`點擊更換 ${ingredient.key}`:'點擊選擇材料';
      if(ingredient){const img=document.createElement('img');loadMinecraftIcon(img,ingredient.key);const label=document.createElement('small');label.textContent=ingredient.key.replace('minecraft:','');choose.append(img,label);const amount=document.createElement('input');amount.type='number';amount.min='1';amount.max='999';amount.value=ingredient.amount||1;amount.title='需要數量';amount.addEventListener('input',event=>{event.stopPropagation();ingredient.amount=Math.max(1,Number(amount.value)||1);syncJson()});const clear=document.createElement('button');clear.type='button';clear.className='slot-clear';clear.textContent='×';clear.title='清除這一格';clear.addEventListener('click',event=>{event.stopPropagation();recipe.ingredients.splice(recipe.ingredients.indexOf(ingredient),1);syncJson();renderVisualFields()});cell.append(choose,amount,clear)}else{choose.className='empty-slot';choose.textContent='＋';cell.append(choose)}
      choose.addEventListener('click',()=>openMaterialPicker(null,id=>{if(ingredient){ingredient.key=id;ingredient.content=false}else recipe.ingredients.push({key:id,amount:1,content:false,slot});syncJson();renderVisualFields()}));grid.append(cell)}
    const arrow=document.createElement('div');arrow.className='crafting-arrow';arrow.textContent='➡';const output=document.createElement('div');output.className='crafting-output';const outputButton=document.createElement('button');outputButton.type='button';const outputImg=document.createElement('img');loadMinecraftIcon(outputImg,itemRecipe?state.editing.base_item:recipe.output);const outputName=document.createElement('small');outputName.textContent=itemRecipe?(state.editing.name||state.key):(recipe.output||'選擇產物');outputButton.append(outputImg,outputName);if(!itemRecipe)outputButton.addEventListener('click',()=>openMaterialPicker(null,id=>{recipe.output=id;syncJson();renderVisualFields()}));else{outputButton.disabled=true;outputButton.title='產物固定為目前編輯的物品'}const outputAmount=document.createElement('input');outputAmount.type='number';outputAmount.min='1';outputAmount.max='99';outputAmount.value=recipe.amount||1;outputAmount.title='產出數量';outputAmount.addEventListener('input',()=>{recipe.amount=Math.max(1,Number(outputAmount.value)||1);syncJson()});output.append(outputButton,outputAmount);bench.append(grid,arrow,output);card.append(bench);
    if(!itemRecipe){const custom=document.createElement('label');custom.className='crafting-custom-output';custom.textContent='產物代碼（也可輸入 CrestRPG 自訂物品代碼）';const customInput=document.createElement('input');customInput.value=recipe.output||'';customInput.addEventListener('input',()=>{recipe.output=customInput.value.trim();syncJson()});custom.append(customInput);card.append(custom)}section.append(card)
  });container.append(section)
}
function craftingInput(labelText,value,onInput,type='text'){const label=document.createElement('label');label.textContent=labelText;const input=document.createElement('input');input.type=type;input.value=value;input.addEventListener('input',()=>{onInput(input.value);syncJson()});label.append(input);return label}
function loadMinecraftIcon(image,id){image.alt=id||'';if(!id||!id.startsWith('minecraft:')){image.className='custom-content-icon';image.alt='自訂物品';return}fetch(`/api/minecraft-assets/icon?id=${encodeURIComponent(id)}`,{headers:{Authorization:`Bearer ${state.token}`}}).then(response=>response.ok?response.blob():null).then(blob=>{if(blob)image.src=URL.createObjectURL(blob)})}
function setPath(root,path,value){let target=root;for(let i=0;i<path.length-1;i++)target=target[path[i]];target[path.at(-1)]=value}
function choiceSelect(choices,value,path){const select=document.createElement('select');select.className='field-select';for(const [key,label] of Object.entries(choices)){const option=document.createElement('option');option.value=key;option.textContent=`${label} (${key})`;select.append(option)}select.value=value;select.addEventListener('change',()=>{setPath(state.editing,path,select.value);syncJson()});return select}
function syncJson(){$('draft-json').value=JSON.stringify(state.editing,null,2);$('json-status').textContent='JSON 格式正確';$('json-status').className='field-help'}
function templateFrom(value){if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([key,item])=>[key,typeof item==='number'?0:typeof item==='boolean'?false:Array.isArray(item)?[]:typeof item==='object'?{}:'']));return typeof value==='number'?0:''}
function humanize(key){return `${key}（此設定欄位的值）`}

function checkJson(){
  try{const value=JSON.parse($('draft-json').value);if(!value||Array.isArray(value)||typeof value!=='object')throw 0;$('json-status').textContent='JSON 格式正確';$('json-status').className='field-help';return value}
  catch{$('json-status').textContent='JSON 格式錯誤，請修正後再儲存';$('json-status').className='field-help error';return null}
}

function openKeyDialog(mode){
  state.mode=mode;$('draft-key').value=mode==='duplicate'?`${state.key}_copy`:'';
  $('dialog-eyebrow').textContent=mode==='duplicate'?'複製草稿':'新增草稿';$('dialog-title').textContent=`設定${labels[state.kind]||state.kind}代碼`;
  $('key-confirm').textContent=mode==='duplicate'?'複製':'建立';$('key-dialog').showModal();$('draft-key').focus();
}

async function createFromDialog(){
  const key=$('draft-key').value.trim().toLowerCase();if(!/^[a-z0-9_-]{1,64}$/.test(key))return;
  if(state.drafts[key]){toast('這個代碼已存在');return}
  const payload=state.mode==='duplicate'?structuredClone(state.drafts[state.key]):structuredClone(defaults[state.kind]||{name:'未命名內容',enabled:true});
  payload.key=key;await api(`/api/draft?kind=${encodeURIComponent(state.kind)}&key=${encodeURIComponent(key)}`,{method:'PUT',body:JSON.stringify(payload)});
  $('key-dialog').close();await reloadCurrent(key);toast(state.mode==='duplicate'?'草稿已複製':'草稿已建立');
}

async function reloadCurrent(selectKey=state.key){
  const data=await api(`/api/drafts?kind=${encodeURIComponent(state.kind)}`);state.drafts=data.drafts||{};await refreshKinds();renderList();
  if(selectKey&&state.drafts[selectKey])openDraft(selectKey);else{$('editor-form').classList.add('hidden');$('editor-placeholder').classList.remove('hidden')}
}

async function validateAll(){
  let result;try{result=await api('/api/validate',{method:'POST'})}catch(error){if(error.status===422)result=error.payload;else throw error}
  $('validation-title').textContent=result.valid?'驗證成功':'發現需要修正的內容';
  if(result.valid){$('validation-results').innerHTML='<p>所有草稿格式與引用關係皆正確。</p>'}
  else{const list=document.createElement('ul');list.className='validation-list';for(const message of result.errors){const li=document.createElement('li');li.textContent=message;list.append(li)}$('validation-results').replaceChildren(list)}
  $('validation-dialog').showModal();
}

async function loadAssets(render=true){const data=await api('/api/assets');state.assets=data.assets||[];if(render)await renderAssets()}
async function openMaterialPicker(path,onSelect=null){state.materialPath=path;state.materialApply=onSelect;state.materialCategory='all';state.materialLimit=80;$('material-search').value='';document.querySelectorAll('#material-category button').forEach(button=>button.classList.toggle('active',button.dataset.category==='all'));$('material-dialog').showModal();$('material-status').textContent='正在載入圖鑑…';try{const data=await api('/api/minecraft-assets');state.minecraftIcons=data.icons||[];$('material-status').textContent=data.ready?`共 ${state.minecraftIcons.length} 個原版材質（全版收錄）`:'尚未同步，請點下方「從 Mojang 同步」。';renderMaterialGrid()}catch(error){$('material-status').textContent=error.message}}
async function openEntityPicker(path){await openMaterialPicker(null,id=>{const entity=id.replace('minecraft:','').replace(/_spawn_egg$/,'');setPath(state.editing,path,`minecraft:${entity}`);syncJson();renderVisualFields()});$('material-search').value='spawn_egg';state.materialCategory='all';renderMaterialGrid()}
function filteredMaterials(){const query=$('material-search').value.trim().toLowerCase();return state.minecraftIcons.filter(icon=>(state.materialCategory==='all'||icon.category===state.materialCategory)&&(!query||icon.id.toLowerCase().includes(query)||icon.name.toLowerCase().includes(query)))}
function renderMaterialGrid(){const matches=filteredMaterials();const values=matches.slice(0,state.materialLimit);const root=$('material-grid');root.replaceChildren();$('material-status').textContent=`共 ${matches.length} 個原版材質（全版收錄）`;for(const icon of values){const button=document.createElement('button');button.type='button';button.className='material-card';const image=document.createElement('img');image.alt=icon.name;image.loading='lazy';fetch(`/api/minecraft-assets/icon?id=${encodeURIComponent(icon.id)}`,{headers:{Authorization:`Bearer ${state.token}`}}).then(response=>response.ok?response.blob():null).then(blob=>{if(blob)image.src=URL.createObjectURL(blob)});const name=document.createElement('strong');name.textContent=icon.name;const code=document.createElement('code');code.textContent=icon.id.replace('minecraft:','');button.append(image,name,code);button.title=`${icon.name} · ${icon.id}`;button.addEventListener('click',()=>{if(state.materialApply)state.materialApply(icon.id);else setPath(state.editing,state.materialPath,icon.id);state.materialApply=null;syncJson();renderVisualFields();$('material-dialog').close();toast(`已選擇 ${icon.name}`)});root.append(button)}if(values.length===0){const empty=document.createElement('p');empty.className='field-help';empty.textContent=state.minecraftIcons.length?'找不到符合條件的材質。':'請先從 Mojang 同步圖示。';root.append(empty)}}
async function syncMinecraftIcons(){$('material-status').textContent='正在從 Mojang 官方資源同步，請稍候…';const result=await api('/api/minecraft-assets/sync',{method:'POST'});const data=await api('/api/minecraft-assets');state.minecraftIcons=data.icons||[];$('material-status').textContent=`已同步 Minecraft ${result.version}，共 ${result.count} 個圖示。`;renderMaterialGrid()}
async function renderAssets(){
  const root=$('asset-list');root.replaceChildren();
  if(state.assets.length===0){const empty=document.createElement('p');empty.className='field-help';empty.textContent='還沒有素材，請上傳 PNG 貼圖或模型 JSON。';root.append(empty);return}
  for(const asset of state.assets){const card=document.createElement('article');card.className='asset-card';
    if(asset.type==='texture'){const img=document.createElement('img');img.alt=asset.name;fetch(`/api/asset/content?type=texture&name=${encodeURIComponent(asset.name)}`,{headers:{Authorization:`Bearer ${state.token}`}}).then(r=>r.blob()).then(blob=>img.src=URL.createObjectURL(blob));card.append(img)}
    const name=document.createElement('strong');name.textContent=asset.name;const code=document.createElement('code');code.textContent=`${asset.type}（${asset.type==='texture'?'物品貼圖':'物品模型'}） · ${formatBytes(asset.size)}（檔案容量）`;const remove=document.createElement('button');remove.className='button danger';remove.textContent='刪除';remove.addEventListener('click',()=>deleteAsset(asset));card.append(name,code,remove);root.append(card)}
}
async function uploadAsset(file,type){if(!file)return null;if(file.size>5*1024*1024){toast('檔案超過 5 MB');return null}const response=await fetch(`/api/asset?type=${type}&name=${encodeURIComponent(file.name)}`,{method:'POST',headers:{Authorization:`Bearer ${state.token}`},body:file});const payload=await response.json();if(!response.ok)throw new Error(payload.error||'上傳失敗');await loadAssets(false);toast('素材已上傳');return payload}
async function deleteAsset(asset){if(!confirm(`刪除素材 ${asset.name}？`))return;await api(`/api/asset?type=${asset.type}&name=${encodeURIComponent(asset.name)}`,{method:'DELETE'});await loadAssets();toast('素材已刪除')}
async function buildPack(){const result=await api('/api/resource-pack/build',{method:'POST'});$('pack-status').textContent=`${formatBytes(result.size)}（檔案容量） · SHA-1（檔案驗證碼）${result.sha1}`;$('download-pack').disabled=false;if(state.key)await reloadCurrent(state.key);toast('資源包已建立，CustomModelData（自訂模型編號）已同步')}
async function downloadPack(){const response=await fetch('/api/resource-pack',{headers:{Authorization:`Bearer ${state.token}`}});if(!response.ok){const data=await response.json();throw new Error(data.error||'下載失敗')}const blob=await response.blob();const link=document.createElement('a');link.href=URL.createObjectURL(blob);link.download='CrestRPG-ResourcePack.zip';link.click();setTimeout(()=>URL.revokeObjectURL(link.href),1000)}
function formatBytes(value){if(value<1024)return `${value} B`;if(value<1048576)return `${(value/1024).toFixed(1)} KB`;return `${(value/1048576).toFixed(1)} MB`}

let toastTimer;function toast(message){$('toast').textContent=message;$('toast').classList.add('show');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').classList.remove('show'),2600)}
function fail(error){if(error.status===401){sessionStorage.removeItem('crest-rpg-token');$('app').classList.add('hidden');$('login').classList.remove('hidden');$('login-error').textContent='管理 Token 已失效，請重新輸入。'}else toast(error.message||'發生錯誤')}

$('login-form').addEventListener('submit',event=>{event.preventDefault();$('login-error').textContent='';login($('token').value).catch(error=>$('login-error').textContent=error.message)});
$('logout').addEventListener('click',()=>{sessionStorage.removeItem('crest-rpg-token');location.reload()});
$('change-token').addEventListener('click',()=>{$('new-token').value='';$('confirm-token').value='';$('token-error').textContent='';$('token-dialog').showModal();$('new-token').focus()});
$('token-cancel').addEventListener('click',()=>$('token-dialog').close());
$('token-form').addEventListener('submit',event=>{event.preventDefault();const next=$('new-token').value;const confirmation=$('confirm-token').value;$('token-error').textContent='';if(next.length<16){$('token-error').textContent='Token 至少需要 16 字元。';return}if(next!==confirmation){$('token-error').textContent='兩次輸入的 Token 不一致。';return}api('/api/token',{method:'PUT',body:JSON.stringify({token:next})}).then(()=>{state.token=next;sessionStorage.setItem('crest-rpg-token',next);$('token-dialog').close();toast('Token 已更新並立即生效')}).catch(error=>$('token-error').textContent=error.message)});
$('search').addEventListener('input',renderList);$('new-draft').addEventListener('click',()=>openKeyDialog('create'));$('duplicate').addEventListener('click',()=>openKeyDialog('duplicate'));
$('draft-json').addEventListener('input',checkJson);$('draft-json').addEventListener('change',()=>{const value=checkJson();if(value){state.editing=value;renderVisualFields()}});
$('editor-form').addEventListener('submit',event=>{event.preventDefault();const payload=checkJson();if(!payload)return;payload.key=state.key;$('save-status').textContent='儲存並套用中…';api(`/api/draft?kind=${encodeURIComponent(state.kind)}&key=${encodeURIComponent(state.key)}`,{method:'PUT',body:JSON.stringify(payload)}).then(()=>api('/api/apply',{method:'POST'})).then(result=>reloadCurrent(state.key).then(()=>result)).then(result=>{$('save-status').textContent='已儲存並套用';toast(`已上傳至插件，熱載入 ${result.count} 筆內容`)}).catch(fail)});
$('delete').addEventListener('click',()=>{if(!state.key||!confirm(`確定要刪除 ${state.key} 嗎？此操作無法復原。`))return;api(`/api/draft?kind=${encodeURIComponent(state.kind)}&key=${encodeURIComponent(state.key)}`,{method:'DELETE'}).then(()=>{state.key=null;return reloadCurrent(null)}).then(()=>toast('草稿已刪除')).catch(fail)});
$('key-form').addEventListener('submit',event=>{event.preventDefault();createFromDialog().catch(fail)});$('key-cancel').addEventListener('click',()=>$('key-dialog').close());$('validate').addEventListener('click',()=>validateAll().catch(fail));$('validation-close').addEventListener('click',()=>$('validation-dialog').close());
$('assets').addEventListener('click',()=>{loadAssets().then(()=>$('assets-dialog').showModal()).catch(fail)});$('assets-close').addEventListener('click',()=>$('assets-dialog').close());$('texture-upload').addEventListener('change',event=>uploadAsset(event.target.files[0],'texture').catch(fail));$('model-upload').addEventListener('change',event=>uploadAsset(event.target.files[0],'model').catch(fail));$('build-pack').addEventListener('click',()=>buildPack().catch(fail));$('download-pack').addEventListener('click',()=>downloadPack().catch(fail));
$('material-close').addEventListener('click',()=>$('material-dialog').close());$('material-search').addEventListener('input',()=>{state.materialLimit=80;renderMaterialGrid()});$('material-category').addEventListener('click',event=>{const button=event.target.closest('button[data-category]');if(!button)return;state.materialCategory=button.dataset.category;state.materialLimit=80;document.querySelectorAll('#material-category button').forEach(item=>item.classList.toggle('active',item===button));renderMaterialGrid()});$('material-grid').addEventListener('scroll',event=>{const root=event.currentTarget;if(root.scrollTop+root.clientHeight>=root.scrollHeight-80&&state.materialLimit<filteredMaterials().length){state.materialLimit+=80;renderMaterialGrid();root.scrollTop=Math.max(0,root.scrollHeight-root.clientHeight-180)}});$('sync-minecraft').addEventListener('click',()=>syncMinecraftIcons().catch(error=>$('material-status').textContent=error.message));
if(state.token)login(state.token).catch(()=>sessionStorage.removeItem('crest-rpg-token'));
