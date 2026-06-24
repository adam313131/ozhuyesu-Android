document.addEventListener('DOMContentLoaded', () => {
    const modalTriggers = document.querySelectorAll('.modal-trigger');
    const modals = document.querySelectorAll('.modal');
    const modalCloseButtons = document.querySelectorAll('.modal-close');
    const modalOverlays = document.querySelectorAll('.modal-overlay');
  
    // --- Function to open a modal ---
    function openModal(modalId) {
      const modal = document.getElementById(modalId);
      if (modal) {
        modal.classList.add('is-open');
        document.body.classList.add('modal-is-open'); // Prevent background scroll
        // Optional: Focus the first focusable element (e.g., close button)
        const focusableElement = modal.querySelector('button.modal-close') || modal.querySelector('a, button');
        if (focusableElement) {
            setTimeout(() => focusableElement.focus(), 50); // Delay helps ensure transition finishes
        }
        // Add event listener for Escape key specific to this open modal
        document.addEventListener('keydown', handleEscapeKey);
      } else {
        console.warn(`Modal with ID "${modalId}" not found.`);
      }
    }
  
    // --- Function to close the currently open modal ---
    function closeModal() {
      const openModal = document.querySelector('.modal.is-open');
      if (openModal) {
        openModal.classList.remove('is-open');
        document.body.classList.remove('modal-is-open'); // Restore background scroll
        // Remove Escape key listener when modal closes
        document.removeEventListener('keydown', handleEscapeKey);
         // Optional: Return focus to the trigger element (more complex, requires storing trigger)
      }
    }
  
    // --- Function to handle Escape key press ---
    function handleEscapeKey(event) {
      if (event.key === 'Escape') {
        closeModal();
      }
    }
  
    // --- Add event listeners to trigger buttons ---
    modalTriggers.forEach(trigger => {
      trigger.addEventListener('click', (event) => {
        event.preventDefault(); // Prevent default link behavior
        const modalId = trigger.getAttribute('data-modal-target');
        if (modalId) {
          openModal(modalId);
        } else {
           console.error("Trigger button missing 'data-modal-target' attribute:", trigger);
        }
      });
    });
  
    // --- Add event listeners to close buttons ---
    modalCloseButtons.forEach(button => {
      button.addEventListener('click', () => {
        closeModal();
      });
    });
  
    // --- Add event listeners to overlays ---
    modalOverlays.forEach(overlay => {
      overlay.addEventListener('click', () => {
        closeModal();
      });
    });
  
  }); // End DOMContentLoaded
  //新增
  document.addEventListener('DOMContentLoaded', () => {
    // --- Theme Toggle Logic (保持不变) ---
    const themeToggle = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;
    function applyTheme(theme) {
      if (theme === 'dark') {
        htmlElement.classList.add('theme-dark');
        htmlElement.classList.remove('theme-auto');
        themeToggle.checked = true;
      } else if (theme === 'light') {
        htmlElement.classList.remove('theme-dark');
        htmlElement.classList.remove('theme-auto');
        themeToggle.checked = false;
      } else {
        htmlElement.classList.add('theme-auto');
        htmlElement.classList.remove('theme-dark');
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
          themeToggle.checked = true;
        } else {
          themeToggle.checked = false;
        }
      }
    }
    const savedTheme = localStorage.getItem('theme') || 'auto';
    applyTheme(savedTheme);
    themeToggle.addEventListener('change', function() {
      if (this.checked) { applyTheme('dark'); localStorage.setItem('theme', 'dark'); }
      else { applyTheme('light'); localStorage.setItem('theme', 'light'); }
    });
    if (window.matchMedia) {
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (localStorage.getItem('theme') === 'auto' || !localStorage.getItem('theme')) {
          if (e.matches) { themeToggle.checked = true; } else { themeToggle.checked = false; }
        }
      });
    }

    // --- Acceleration Toggle Logic (修改后) ---
    const accelerationToggle = document.getElementById('acceleration-toggle');
    
    // 定义不同类型链接的配置
    const accelerationConfigs = [
      {
        selector: '.acceleratable-link',
        prefix: 'https://speedp.ingoshen.com/?url=',
        originalHrefDataKey: 'originalHrefTypeA' // 使用不同的data key存储原始链接
      },
      {
        selector: '.acceleratable-linkb',
        prefix: 'https://speedw.ingoshen.com/',
        originalHrefDataKey: 'originalHrefTypeB'
      }
      // 可以添加更多配置项
    ];

    // Function to store original hrefs for all configured link types
    function storeAllOriginalHrefs() {
      accelerationConfigs.forEach(config => {
        const links = document.querySelectorAll(config.selector);
        links.forEach(link => {
          if (!link.dataset[config.originalHrefDataKey]) {
            if (link.getAttribute('href') !== '#') {
              link.dataset[config.originalHrefDataKey] = link.getAttribute('href');
            }
          }
        });
      });
    }

// --- Language Toggle Logic (OpenCC - 按需懒加载优化版) ---
    const langToggle = document.getElementById('lang-toggle');
    let converterCN2TW = null;
    let converterTW2CN = null;
    let isFullJsLoaded = false; // 标记 full.js 是否已加载

    // 1. 定义动态加载 full.js 的函数
    function loadFullJsLibrary(callback) {
        if (typeof OpenCC !== 'undefined') {
            isFullJsLoaded = true;
            if (callback) callback();
            return;
        }
        
        // 创建 script 标签
        const script = document.createElement('script');
        script.src = 'js/full.js';
        script.onload = () => {
            isFullJsLoaded = true;
            // 初始化转换器
            if (typeof OpenCC !== 'undefined') {
                converterCN2TW = OpenCC.Converter({ from: 'cn', to: 'tw' });
                converterTW2CN = OpenCC.Converter({ from: 'tw', to: 'cn' });
            }
            if (callback) callback();
        };
        document.body.appendChild(script);
    }

    // 2. DOM 遍历与文本替换函数 (保持不变)
    function traverseAndConvert(node, converter) {
        if (node.nodeType === 3) { // Text node
            const originalText = node.nodeValue;
            if (originalText.trim().length > 0) {
                const convertedText = converter(originalText);
                if (convertedText !== originalText) {
                    node.nodeValue = convertedText;
                }
            }
        } else if (node.nodeType === 1) { // Element node
            if (node.tagName === 'SCRIPT' || 
                node.tagName === 'STYLE' || 
                node.classList.contains('no-translate')) {
                return;
            }
            for (let i = 0; i < node.childNodes.length; i++) {
                traverseAndConvert(node.childNodes[i], converter);
            }
        }
    }

    // 3. 应用语言设置的主函数
    function applyLanguage(toTraditional) {
        // 确保转换器已初始化
        if (!converterCN2TW || !converterTW2CN) {
            if (typeof OpenCC !== 'undefined') {
                converterCN2TW = OpenCC.Converter({ from: 'cn', to: 'tw' });
                converterTW2CN = OpenCC.Converter({ from: 'tw', to: 'cn' });
            } else {
                return; // 库还未加载，无法转换
            }
        }

        const converter = toTraditional ? converterCN2TW : converterTW2CN;
        
        traverseAndConvert(document.body, converter);
        document.title = converter(document.title);
        
        const metaDesc = document.querySelector('meta[name="description"]');
        if (metaDesc) {
            metaDesc.setAttribute('content', converter(metaDesc.getAttribute('content')));
        }
    }

    // 4. 初始化逻辑
    const savedLang = localStorage.getItem('langPref');

    // 情况 A: 如果用户之前选择了繁体，我们需要立即(异步)加载 full.js 并转换
    if (savedLang === 'traditional') {
        langToggle.checked = true;
        // 页面显示 loading 状态或直接加载
        loadFullJsLibrary(() => {
            applyLanguage(true);
        });
    }

    // 5. 监听切换事件
    if (langToggle) {
        langToggle.addEventListener('change', function() {
            const isTraditional = this.checked;

            if (isTraditional && !isFullJsLoaded) {
                // 用户首次点击繁体，需要下载 full.js
                // 这里可以加一个 loading 动画提示用户正在下载语言包
                const originalCursor = document.body.style.cursor;
                document.body.style.cursor = 'wait'; // 鼠标变成等待状态
                
                loadFullJsLibrary(() => {
                    applyLanguage(true);
                    document.body.style.cursor = originalCursor; // 恢复鼠标
                });
            } else {
                // 已经加载过，或者是切回简体（通常切回简体不需要库，但这里转换器需要库支持）
                // 如果是切回简体(isTraditional=false)，且库没加载，其实不需要做任何事(本来就是简体)
                // 如果库已加载，则执行转换
                if (isFullJsLoaded) {
                    applyLanguage(isTraditional);
                }
            }
            
            localStorage.setItem('langPref', isTraditional ? 'traditional' : 'simplified');
        });
    }
    // Function to apply or remove acceleration for all configured link types
    function applyAllAcceleration(isAccelerated) {
      accelerationConfigs.forEach(config => {
        const links = document.querySelectorAll(config.selector);
        links.forEach(link => {
          const originalHref = link.dataset[config.originalHrefDataKey];
          const currentHref = link.getAttribute('href');

          if (originalHref) { // 只处理存储了原始链接的
            if (isAccelerated) {
              // 确保不会重复添加前缀，并且当前链接不是另一个加速配置的前缀
              let alreadyPrefixedByOther = false;
              accelerationConfigs.forEach(otherConfig => {
                  if (otherConfig.prefix !== config.prefix && currentHref && currentHref.startsWith(otherConfig.prefix)) {
                      alreadyPrefixedByOther = true;
                  }
              });

              if (currentHref && !currentHref.startsWith(config.prefix) && !alreadyPrefixedByOther) {
                link.setAttribute('href', config.prefix + originalHref);
              }
            } else {
              // 恢复时，确保只恢复由这个配置修改的链接
              if (currentHref && currentHref.startsWith(config.prefix)) {
                link.setAttribute('href', originalHref);
              } else if (!currentHref || !accelerationConfigs.some(ac => currentHref.startsWith(ac.prefix))) {
                // 如果当前链接没有已知的加速前缀 (可能是直接的原始链接)，则确保它是原始链接
                link.setAttribute('href', originalHref);
              }
            }
          }
        });
      });
    }

    // Initial setup
    storeAllOriginalHrefs();
    const savedAccelerationState = localStorage.getItem('acceleration') === 'true';
    accelerationToggle.checked = savedAccelerationState;
    applyAllAcceleration(savedAccelerationState);

    // Event listener for the acceleration toggle
    accelerationToggle.addEventListener('change', function() {
      const isAccelerated = this.checked;
      applyAllAcceleration(isAccelerated);
      localStorage.setItem('acceleration', isAccelerated);
    });

    // Modal handling
    const modalTriggers = document.querySelectorAll('.modal-trigger');
    modalTriggers.forEach(trigger => {
        trigger.addEventListener('click', () => {
            setTimeout(() => { // Delay for modal to render
                // 当模态框打开时，确保其内部所有可加速链接的原始href已存储
                storeAllOriginalHrefs();
                // 然后根据当前加速状态更新这些链接
                applyAllAcceleration(accelerationToggle.checked);
            }, 100);
        });
    });

  });
  