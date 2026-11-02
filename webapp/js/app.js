/**
 * TaskVerse — Frontend Application Controller
 * Connects the Midnight Theme Dashboard with the Java/Tomcat Backend & MySQL Database.
 */

// Determine application context path dynamically
const CONTEXT_PATH = window.location.pathname.includes('/TaskVerse') ? '/TaskVerse' : '';
const API = {
    TASKS: `${CONTEXT_PATH}/api/tasks`,
    DASHBOARD: `${CONTEXT_PATH}/api/dashboard`,
    HEALTH: `${CONTEXT_PATH}/api/health`,
    NOTES: `${CONTEXT_PATH}/api/notes`,
    PRODUCTIVITY: `${CONTEXT_PATH}/api/productivity`,
    AUTH: `${CONTEXT_PATH}/api/auth`
};

// Application State

// Authentication & CSRF Request Headers
function getAuthHeaders() {
    const h = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
    if (state && state.csrfToken) {
        h['X-CSRF-Token'] = state.csrfToken;
    }
    return h;
}

let state = {
    tasks: [],
    notes: [],
    notesSearchQuery: '',
    noteToDeleteId: null,
    noteSaveTimers: {},
    filterStatus: 'all',
    filterPriority: 'all',
    filterCategory: 'all',
    searchQuery: '',
    taskToDeleteId: null,
    draggedTaskId: null,
    draggedFromStatus: null,
    dismissedAlerts: { overdue: false, upcoming: false },
    currentView: 'dashboard', // 'dashboard' | 'tasks' | 'calendar' | 'notes' | 'productivity'
    calendarYear: new Date().getFullYear(),
    calendarMonth: new Date().getMonth(), // 0-indexed (0 = Jan, 9 = Oct)
    productivityYear: new Date().getFullYear(),
    productivityMonth: new Date().getMonth() + 1, // 1-indexed for backend API (1 = Jan, 10 = Oct)
    productivityData: null,
    totalPoints: 0,
    celebratedMilestones: new Set(),
    currentUser: null,
    csrfToken: null
};

// DOM Elements
const elements = {
    appLayout: document.getElementById('appLayout'),
    appSidebar: document.getElementById('appSidebar'),
    sidebarToggleBtn: document.getElementById('sidebarToggleBtn'),
    mobileMenuBtn: document.getElementById('mobileMenuBtn'),
    sidebarCloseBtn: document.getElementById('sidebarCloseBtn'),
    sidebarBackdrop: document.getElementById('sidebarBackdrop'),
    pageTitle: document.getElementById('pageTitle'),
    pageSubtitle: document.getElementById('pageSubtitle'),
    viewDashboard: document.getElementById('viewDashboard'),
    viewTasks: document.getElementById('viewTasks'),
    viewCalendar: document.getElementById('viewCalendar'),
    viewNotes: document.getElementById('viewNotes'),
    viewProductivity: document.getElementById('viewProductivity'),
    navDashboard: document.getElementById('navDashboard'),
    navTasks: document.getElementById('navTasks'),
    navCalendar: document.getElementById('navCalendar'),
    navNotes: document.getElementById('navNotes'),
    navProductivity: document.getElementById('navProductivity'),
    topNavDashboard: document.getElementById('topNavDashboard'),
    topNavTasks: document.getElementById('topNavTasks'),
    topNavCalendar: document.getElementById('topNavCalendar'),
    topNavNotes: document.getElementById('topNavNotes'),
    topNavProductivity: document.getElementById('topNavProductivity'),
    prodMonthTitle: document.getElementById('prodMonthTitle'),
    prodMonthBadge: document.getElementById('prodMonthBadge'),
    prodPrevMonthBtn: document.getElementById('prodPrevMonthBtn'),
    prodNextMonthBtn: document.getElementById('prodNextMonthBtn'),
    prodCurrentMonthBtn: document.getElementById('prodCurrentMonthBtn'),
    prodRefreshBtn: document.getElementById('prodRefreshBtn'),
    prodStatCompleted: document.getElementById('prodStatCompleted'),
    prodStatCompletedSubtext: document.getElementById('prodStatCompletedSubtext'),
    prodStatCreated: document.getElementById('prodStatCreated'),
    prodStatCreatedSubtext: document.getElementById('prodStatCreatedSubtext'),
    prodStatDue: document.getElementById('prodStatDue'),
    prodStatDueSubtext: document.getElementById('prodStatDueSubtext'),
    prodStatRate: document.getElementById('prodStatRate'),
    prodStatRateSubtext: document.getElementById('prodStatRateSubtext'),
    prodChartBars: document.getElementById('prodChartBars'),
    prodChartEmpty: document.getElementById('prodChartEmpty'),
    prodChartWrapper: document.getElementById('prodChartWrapper'),
    prodEmptyMonthName: document.getElementById('prodEmptyMonthName'),
    prodLoadingIndicator: document.getElementById('prodLoadingIndicator'),
    prodPeakDayText: document.getElementById('prodPeakDayText'),
    prodPeakDayDesc: document.getElementById('prodPeakDayDesc'),
    prodComparisonText: document.getElementById('prodComparisonText'),
    prodComparisonDesc: document.getElementById('prodComparisonDesc'),
    prodTrendIconBox: document.getElementById('prodTrendIconBox'),
    prodScaleHint: document.getElementById('prodScaleHint'),
    notesCounterBadge: document.getElementById('notesCounterBadge'),
    notesSearchInput: document.getElementById('notesSearchInput'),
    clearNotesSearchBtn: document.getElementById('clearNotesSearchBtn'),
    createNoteBtn: document.getElementById('createNoteBtn'),
    notesLoadingIndicator: document.getElementById('notesLoadingIndicator'),
    notesEmptyState: document.getElementById('notesEmptyState'),
    notesEmptyActionBtn: document.getElementById('notesEmptyActionBtn'),
    notesGrid: document.getElementById('notesGrid'),
    dashboardActiveTasksList: document.getElementById('dashboardActiveTasksList'),
    dashboardUpcomingDeadlinesList: document.getElementById('dashboardUpcomingDeadlinesList'),
    calendarSearchInput: document.getElementById('calendarSearchInput'),
    clearCalSearchBtn: document.getElementById('clearCalSearchBtn'),
    calRefreshBtn: document.getElementById('calRefreshBtn'),
    alertsContainer: document.getElementById('alertsContainer'),
    tasksBoard: document.getElementById('tasksBoard'),
    calendarView: document.getElementById('calendarView'),
    viewBoardBtn: document.getElementById('viewBoardBtn'),
    viewCalendarBtn: document.getElementById('viewCalendarBtn'),
    navBoardItem: document.getElementById('navBoardItem'),
    navCalendarItem: document.getElementById('navCalendarItem'),
    calPrevMonthBtn: document.getElementById('calPrevMonthBtn'),
    calNextMonthBtn: document.getElementById('calNextMonthBtn'),
    calTodayBtn: document.getElementById('calTodayBtn'),
    calMonthYearTitle: document.getElementById('calMonthYearTitle'),
    calScheduledBadge: document.getElementById('calScheduledBadge'),
    calendarDaysGrid: document.getElementById('calendarDaysGrid'),
    unscheduledDrawer: document.getElementById('unscheduledDrawer'),
    unscheduledCount: document.getElementById('unscheduledCount'),
    unscheduledItems: document.getElementById('unscheduledItems'),
    dropzonePending: document.getElementById('dropzonePending'),
    dropzoneInProgress: document.getElementById('dropzoneInProgress'),
    dropzoneCompleted: document.getElementById('dropzoneCompleted'),
    countPending: document.getElementById('countPending'),
    countInProgress: document.getElementById('countInProgress'),
    countCompleted: document.getElementById('countCompleted'),
    tasksGrid: document.getElementById('tasksGrid'),
    loadingIndicator: document.getElementById('loadingIndicator'),
    emptyState: document.getElementById('emptyState'),
    taskCounterText: document.getElementById('taskCounterText'),
    searchInput: document.getElementById('searchInput'),
    clearSearchBtn: document.getElementById('clearSearchBtn'),
    filterStatus: document.getElementById('filterStatus'),
    filterPriority: document.getElementById('filterPriority'),
    filterCategory: document.getElementById('filterCategory'),
    refreshBtn: document.getElementById('refreshBtn'),
    openCreateModalBtn: document.getElementById('openCreateModalBtn'),
    emptyActionBtn: document.getElementById('emptyActionBtn'),
    emptyTitle: document.getElementById('emptyTitle'),
    emptyMessage: document.getElementById('emptyMessage'),
    closeModalBtn: document.getElementById('closeModalBtn'),
    cancelModalBtn: document.getElementById('cancelModalBtn'),
    taskModal: document.getElementById('taskModal'),
    taskForm: document.getElementById('taskForm'),
    modalTitle: document.getElementById('modalTitle'),
    formTaskId: document.getElementById('formTaskId'),
    formTitle: document.getElementById('formTitle'),
    formDescription: document.getElementById('formDescription'),
    formPriority: document.getElementById('formPriority'),
    formStatus: document.getElementById('formStatus'),
    formCategory: document.getElementById('formCategory'),
    formDueDate: document.getElementById('formDueDate'),
    titleError: document.getElementById('titleError'),
    deleteModal: document.getElementById('deleteModal'),
    deleteModalTitle: document.getElementById('deleteModalTitle'),
    deleteModalDesc: document.getElementById('deleteModalDesc'),
    deleteTaskTitle: document.getElementById('deleteTaskTitle'),
    confirmDeleteBtn: document.getElementById('confirmDeleteBtn'),
    confirmDeleteBtnText: document.getElementById('confirmDeleteBtnText'),
    cancelDeleteBtn: document.getElementById('cancelDeleteBtn'),
    statTotal: document.getElementById('statTotal'),
    statPending: document.getElementById('statPending'),
    statInProgress: document.getElementById('statInProgress'),
    statCompleted: document.getElementById('statCompleted'),
    statPoints: document.getElementById('statPoints'),
    statPointsSubtext: document.getElementById('statPointsSubtext'),
    cardTotalPoints: document.getElementById('cardTotalPoints'),
    celebrationCanvas: document.getElementById('celebrationCanvas'),
    celebrationContainer: document.getElementById('celebrationContainer'),
    milestoneModal: document.getElementById('milestoneModal'),
    closeMilestoneBtn: document.getElementById('closeMilestoneBtn'),
    milestoneBadgeTop: document.getElementById('milestoneBadgeTop'),
    milestoneIcon: document.getElementById('milestoneIcon'),
    milestoneTitle: document.getElementById('milestoneTitle'),
    milestoneMessage: document.getElementById('milestoneMessage'),
    milestonePointsTotal: document.getElementById('milestonePointsTotal'),
    completionRateText: document.getElementById('completionRateText'),
    progressCompletedCount: document.getElementById('progressCompletedCount'),
    progressTotalCount: document.getElementById('progressTotalCount'),
    progressPercentValue: document.getElementById('progressPercentValue'),
    progressBarFill: document.getElementById('progressBarFill'),
    progressTrack: document.getElementById('progressTrack'),
    progressRemainingText: document.getElementById('progressRemainingText'),
    progressStatusMessage: document.getElementById('progressStatusMessage'),
    dbStatusBadge: document.getElementById('dbStatusBadge'),
    dbStatusText: document.getElementById('dbStatusText'),
    toastContainer: document.getElementById('toastContainer')
};

// Track initial page load start time for the animated loading screen
const appLoadStartTime = Date.now();
let isInitialLoad = true;

/**
 * Smoothly dismisses the full-screen curtain loading screen
 */
function dismissAppLoader() {
    const loader = document.getElementById('appLoader');
    if (!loader || loader.dataset.dismissed) return;
    loader.dataset.dismissed = 'true';

    const statusText = document.getElementById('loaderStatusText');
    if (statusText) statusText.textContent = 'Ready!';

    // Ensure loader displays for a pleasant minimum duration (approx 950ms total)
    const elapsed = Date.now() - appLoadStartTime;
    const minDisplayTime = 950;
    const remainingWait = Math.max(0, minDisplayTime - elapsed);

    setTimeout(() => {
        loader.classList.add('slide-up');

        const cleanup = () => {
            loader.style.display = 'none';
            loader.style.visibility = 'hidden';
            if (loader.parentNode) {
                loader.parentNode.removeChild(loader);
            }
        };

        loader.addEventListener('transitionend', cleanup, { once: true });
        // Fallback cleanup in case transitionend does not fire
        setTimeout(cleanup, 950);
    }, remainingWait);
}

// Initialize application on page load
document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();
    initSidebarPreference();
    initNavigation();
    initCelebrationSystem();
    initAuthEventListeners();
    checkAuthAndInit();

    // Safety fallback: Dismiss loader after 3.2 seconds max so the user is never stuck
    setTimeout(() => {
        dismissAppLoader();
    }, 3200);
});

/**
 * Attach Event Listeners
 */
function initEventListeners() {
    // Modal openers & closers
    elements.openCreateModalBtn.addEventListener('click', () => openTaskModal());
    if (elements.emptyActionBtn) {
        elements.emptyActionBtn.addEventListener('click', () => {
            if (state.searchQuery) {
                clearSearch();
            } else {
                openTaskModal();
            }
        });
    }
    elements.closeModalBtn.addEventListener('click', closeTaskModal);
    elements.cancelModalBtn.addEventListener('click', closeTaskModal);
    
    // Close modal when clicking outside card
    elements.taskModal.addEventListener('click', (e) => {
        if (e.target === elements.taskModal) closeTaskModal();
    });
    elements.deleteModal.addEventListener('click', (e) => {
        if (e.target === elements.deleteModal) closeDeleteModal();
    });
    if (elements.milestoneModal) {
        elements.milestoneModal.addEventListener('click', (e) => {
            if (e.target === elements.milestoneModal) closeMilestoneModal();
        });
    }
    if (elements.closeMilestoneBtn) {
        elements.closeMilestoneBtn.addEventListener('click', closeMilestoneModal);
    }

    // Form submit
    elements.taskForm.addEventListener('submit', handleFormSubmit);

    // Delete modal buttons
    elements.cancelDeleteBtn.addEventListener('click', closeDeleteModal);

    // Desktop sidebar collapse/expand toggle
    if (elements.sidebarToggleBtn) {
        elements.sidebarToggleBtn.addEventListener('click', toggleSidebarCollapse);
    }
    // Keyboard shortcut: Ctrl+B to toggle sidebar
    window.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
            const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
            if (activeTag !== 'input' && activeTag !== 'textarea') {
                e.preventDefault();
                toggleSidebarCollapse();
            }
        }
    });
    elements.confirmDeleteBtn.addEventListener('click', executeDeleteTask);

    // Refresh button
    elements.refreshBtn.addEventListener('click', () => {
        showToast('Refreshing tasks from MySQL...', 'info');
        loadDashboard();
        checkHealth();
    });

    // Search input: updates results as user types (case-insensitive title search)
    elements.searchInput.addEventListener('input', (e) => {
        state.searchQuery = e.target.value.trim().toLowerCase();
        elements.clearSearchBtn.style.display = e.target.value.length > 0 ? 'flex' : 'none';
        if (elements.calendarSearchInput) elements.calendarSearchInput.value = e.target.value;
        if (elements.clearCalSearchBtn) elements.clearCalSearchBtn.style.display = e.target.value.length > 0 ? 'flex' : 'none';
        applyFiltersAndRender();
    });

    // Escape key inside search input clears search
    elements.searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            clearSearch();
        }
    });

    // Clear search button
    elements.clearSearchBtn.addEventListener('click', clearSearch);

    // Select filters
    elements.filterStatus.addEventListener('change', (e) => {
        state.filterStatus = e.target.value;
        highlightActiveFilterCard(state.filterStatus);
        applyFiltersAndRender();
    });
    elements.filterPriority.addEventListener('change', (e) => {
        state.filterPriority = e.target.value;
        applyFiltersAndRender();
    });
    elements.filterCategory.addEventListener('change', (e) => {
        state.filterCategory = e.target.value;
        applyFiltersAndRender();
    });

    // Sidebar quick filter buttons (filters and switches to My Tasks view)
    document.querySelectorAll('[data-status-filter]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const filterVal = btn.getAttribute('data-status-filter');
            state.filterStatus = filterVal;
            if (elements.filterStatus) elements.filterStatus.value = filterVal;
            highlightActiveFilterCard(filterVal);
            navigateTo('tasks');
            applyFiltersAndRender();
        });
    });

    // Dashboard Statistics Cards (Click to filter and switch to My Tasks view)
    document.querySelectorAll('[data-dashboard-filter]').forEach(card => {
        const handleFilter = () => {
            const filterVal = card.getAttribute('data-dashboard-filter');
            state.filterStatus = filterVal;
            if (elements.filterStatus) elements.filterStatus.value = filterVal;
            highlightActiveFilterCard(filterVal);
            navigateTo('tasks');
            applyFiltersAndRender();
        };
        card.addEventListener('click', handleFilter);
        card.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleFilter();
            }
        });
    });

    // Calendar month navigation
    if (elements.calPrevMonthBtn) {
        elements.calPrevMonthBtn.addEventListener('click', () => {
            state.calendarMonth--;
            if (state.calendarMonth < 0) {
                state.calendarMonth = 11;
                state.calendarYear--;
            }
            renderCalendar();
        });
    }
    if (elements.calNextMonthBtn) {
        elements.calNextMonthBtn.addEventListener('click', () => {
            state.calendarMonth++;
            if (state.calendarMonth > 11) {
                state.calendarMonth = 0;
                state.calendarYear++;
            }
            renderCalendar();
        });
    }
    if (elements.calTodayBtn) {
        elements.calTodayBtn.addEventListener('click', () => {
            const today = new Date();
            state.calendarYear = today.getFullYear();
            state.calendarMonth = today.getMonth();
            renderCalendar();
        });
    }

    // Sticky Notes Event Listeners
    if (elements.createNoteBtn) {
        elements.createNoteBtn.addEventListener('click', () => createNewNote());
    }
    if (elements.notesEmptyActionBtn) {
        elements.notesEmptyActionBtn.addEventListener('click', () => createNewNote());
    }
    if (elements.notesSearchInput) {
        elements.notesSearchInput.addEventListener('input', (e) => {
            state.notesSearchQuery = e.target.value.trim().toLowerCase();
            if (elements.clearNotesSearchBtn) {
                elements.clearNotesSearchBtn.style.display = e.target.value.length > 0 ? 'flex' : 'none';
            }
            renderNotes();
        });
        elements.notesSearchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                clearNotesSearch();
            }
        });
    }
    if (elements.clearNotesSearchBtn) {
        elements.clearNotesSearchBtn.addEventListener('click', clearNotesSearch);
    }

    // Monthly Productivity Tracker Month Navigation Controls
    if (elements.prodPrevMonthBtn) {
        elements.prodPrevMonthBtn.addEventListener('click', () => {
            state.productivityMonth--;
            if (state.productivityMonth < 1) {
                state.productivityMonth = 12;
                state.productivityYear--;
            }
            loadProductivity();
        });
    }
    if (elements.prodNextMonthBtn) {
        elements.prodNextMonthBtn.addEventListener('click', () => {
            state.productivityMonth++;
            if (state.productivityMonth > 12) {
                state.productivityMonth = 1;
                state.productivityYear++;
            }
            loadProductivity();
        });
    }
    if (elements.prodCurrentMonthBtn) {
        elements.prodCurrentMonthBtn.addEventListener('click', () => {
            const today = new Date();
            state.productivityYear = today.getFullYear();
            state.productivityMonth = today.getMonth() + 1;
            loadProductivity();
        });
    }
    if (elements.prodRefreshBtn) {
        elements.prodRefreshBtn.addEventListener('click', () => {
            showToast('Refreshing productivity metrics from database...', 'info');
            loadProductivity();
        });
    }

    // Initialize Kanban drag & drop zones
    initKanbanDropzones();
}

/**
 * Initializes Navigation System Event Listeners
 */
function initNavigation() {
    // 1. Sidebar Navigation Links
    const sidebarLinks = [
        { el: elements.navDashboard, view: 'dashboard' },
        { el: elements.navTasks, view: 'tasks' },
        { el: elements.navCalendar, view: 'calendar' },
        { el: elements.navNotes, view: 'notes' },
        { el: elements.navProductivity, view: 'productivity' }
    ];
    sidebarLinks.forEach(({ el, view }) => {
        if (!el) return;
        el.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(view);
        });
    });

    // 2. Unified Top Navigation Tabs
    const topNavTabs = [
        { el: elements.topNavDashboard, view: 'dashboard' },
        { el: elements.topNavTasks, view: 'tasks' },
        { el: elements.topNavCalendar, view: 'calendar' },
        { el: elements.topNavNotes, view: 'notes' },
        { el: elements.topNavProductivity, view: 'productivity' }
    ];
    topNavTabs.forEach(({ el, view }) => {
        if (!el) return;
        el.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(view);
        });
    });

    // 3. View Switcher buttons (if used)
    if (elements.viewBoardBtn) {
        elements.viewBoardBtn.addEventListener('click', () => navigateTo('tasks'));
    }
    if (elements.viewCalendarBtn) {
        elements.viewCalendarBtn.addEventListener('click', () => navigateTo('calendar'));
    }

    // 4. Browser Back/Forward buttons hash router
    window.addEventListener('hashchange', () => {
        navigateTo(getRouteFromHash(), false);
    });

    // 5. Calendar search toolbar input
    if (elements.calendarSearchInput) {
        elements.calendarSearchInput.addEventListener('input', (e) => {
            state.searchQuery = e.target.value.trim().toLowerCase();
            if (elements.searchInput) elements.searchInput.value = e.target.value;
            if (elements.clearCalSearchBtn) {
                elements.clearCalSearchBtn.style.display = e.target.value.length > 0 ? 'flex' : 'none';
            }
            if (elements.clearSearchBtn) {
                elements.clearSearchBtn.style.display = e.target.value.length > 0 ? 'flex' : 'none';
            }
            renderCalendar();
        });
    }

    if (elements.clearCalSearchBtn) {
        elements.clearCalSearchBtn.addEventListener('click', () => {
            if (elements.calendarSearchInput) elements.calendarSearchInput.value = '';
            if (elements.searchInput) elements.searchInput.value = '';
            state.searchQuery = '';
            elements.clearCalSearchBtn.style.display = 'none';
            if (elements.clearSearchBtn) elements.clearSearchBtn.style.display = 'none';
            renderCalendar();
        });
    }

    if (elements.calRefreshBtn) {
        elements.calRefreshBtn.addEventListener('click', () => {
            showToast('Refreshing tasks from MySQL...', 'info');
            loadDashboard();
        });
    }

    // 6. Mobile Sidebar Drawer Controls
    if (elements.mobileMenuBtn) {
        elements.mobileMenuBtn.addEventListener('click', () => openMobileSidebar());
    }
    if (elements.sidebarCloseBtn) {
        elements.sidebarCloseBtn.addEventListener('click', () => closeMobileSidebar());
    }
    if (elements.sidebarBackdrop) {
        elements.sidebarBackdrop.addEventListener('click', () => closeMobileSidebar());
    }
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeMobileSidebar();
            closeMilestoneModal();
        }
    });

    // Close mobile drawer when clicking any sidebar navigation link or filter button
    document.querySelectorAll('.sidebar-nav .nav-item, .sidebar-nav .nav-filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            closeMobileSidebar();
        });
    });

    // Reset drawer state when window resizes back to desktop
    window.addEventListener('resize', () => {
        if (window.innerWidth > 900) {
            closeMobileSidebar();
        }
    });
}

/**
 * Opens mobile sidebar off-canvas drawer
 */
/**
 * Toggles desktop sidebar collapsed state and saves preference
 */
function toggleSidebarCollapse() {
    const sidebar = elements.appSidebar || document.getElementById('appSidebar');
    const layout = elements.appLayout || document.getElementById('appLayout') || document.querySelector('.app-layout');
    if (!sidebar) return;
    const isCollapsed = sidebar.classList.toggle('collapsed');
    if (layout) {
        layout.classList.toggle('sidebar-collapsed', isCollapsed);
    }
    try {
        localStorage.setItem('taskverse_sidebar_collapsed', isCollapsed ? 'true' : 'false');
    } catch (e) {}

    const toggleBtn = elements.sidebarToggleBtn || document.getElementById('sidebarToggleBtn');
    if (toggleBtn) {
        toggleBtn.setAttribute('title', isCollapsed ? 'Expand sidebar (Ctrl+B)' : 'Collapse sidebar (Ctrl+B)');
        toggleBtn.setAttribute('aria-expanded', isCollapsed ? 'false' : 'true');
        toggleBtn.setAttribute('aria-label', isCollapsed ? 'Expand navigation sidebar' : 'Collapse navigation sidebar');
    }

    // Immediately trigger window resize event for instant dynamic layout recalculation
    window.dispatchEvent(new Event('resize'));
    // Re-dispatch once CSS transition finishes for pixel-perfect settle
    setTimeout(() => {
        window.dispatchEvent(new Event('resize'));
    }, 360);
}

/**
 * Restores desktop sidebar collapsed state from localStorage
 */
function initSidebarPreference() {
    if (window.innerWidth >= 901) {
        try {
            const saved = localStorage.getItem('taskverse_sidebar_collapsed');
            const sidebar = elements.appSidebar || document.getElementById('appSidebar');
            const layout = elements.appLayout || document.getElementById('appLayout') || document.querySelector('.app-layout');
            const toggleBtn = elements.sidebarToggleBtn || document.getElementById('sidebarToggleBtn');

            if (saved === 'true') {
                if (sidebar) sidebar.classList.add('collapsed');
                if (layout) layout.classList.add('sidebar-collapsed');
                if (toggleBtn) {
                    toggleBtn.setAttribute('title', 'Expand sidebar (Ctrl+B)');
                    toggleBtn.setAttribute('aria-expanded', 'false');
                    toggleBtn.setAttribute('aria-label', 'Expand navigation sidebar');
                }
            } else {
                if (sidebar) sidebar.classList.remove('collapsed');
                if (layout) layout.classList.remove('sidebar-collapsed');
                if (toggleBtn) {
                    toggleBtn.setAttribute('title', 'Collapse sidebar (Ctrl+B)');
                    toggleBtn.setAttribute('aria-expanded', 'true');
                    toggleBtn.setAttribute('aria-label', 'Collapse navigation sidebar');
                }
            }
            window.dispatchEvent(new Event('resize'));
        } catch (e) {}
    }
}

function openMobileSidebar() {
    const sidebar = elements.appSidebar || document.getElementById('appSidebar');
    const backdrop = elements.sidebarBackdrop || document.getElementById('sidebarBackdrop');
    const menuBtn = elements.mobileMenuBtn || document.getElementById('mobileMenuBtn');
    if (sidebar) sidebar.classList.add('open');
    if (backdrop) backdrop.classList.add('active');
    if (menuBtn) menuBtn.setAttribute('aria-expanded', 'true');
    document.body.style.overflow = 'hidden';
}

/**
 * Closes mobile sidebar off-canvas drawer
 */
function closeMobileSidebar() {
    const sidebar = elements.appSidebar || document.getElementById('appSidebar');
    const backdrop = elements.sidebarBackdrop || document.getElementById('sidebarBackdrop');
    const menuBtn = elements.mobileMenuBtn || document.getElementById('mobileMenuBtn');
    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.remove('active');
    if (menuBtn) menuBtn.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
}

/**
 * Unified Navigation Controller
 */
function navigateTo(viewName, updateHash = true) {
    // Ensure mobile sidebar is closed whenever view changes
    closeMobileSidebar();

    const validViews = ['dashboard', 'tasks', 'calendar', 'notes', 'productivity'];
    if (!validViews.includes(viewName)) {
        viewName = 'dashboard';
    }

    state.currentView = viewName;

    // 1. Update browser URL hash if requested
    if (updateHash && window.location.hash !== `#${viewName}`) {
        window.location.hash = `#${viewName}`;
    }

    // 2. Toggle visibility of view panels
    if (elements.viewDashboard) {
        elements.viewDashboard.style.display = viewName === 'dashboard' ? 'block' : 'none';
    }
    if (elements.viewTasks) {
        elements.viewTasks.style.display = viewName === 'tasks' ? 'block' : 'none';
    }
    if (elements.viewCalendar) {
        elements.viewCalendar.style.display = viewName === 'calendar' ? 'block' : 'none';
    }
    if (elements.viewNotes) {
        elements.viewNotes.style.display = viewName === 'notes' ? 'block' : 'none';
    }
    if (elements.viewProductivity) {
        elements.viewProductivity.style.display = viewName === 'productivity' ? 'block' : 'none';
    }

    // 3. Update active states on Sidebar Navigation
    const navItems = [
        { el: elements.navDashboard, view: 'dashboard' },
        { el: elements.navTasks, view: 'tasks' },
        { el: elements.navCalendar, view: 'calendar' },
        { el: elements.navNotes, view: 'notes' },
        { el: elements.navProductivity, view: 'productivity' }
    ];
    navItems.forEach(({ el, view }) => {
        if (!el) return;
        const isActive = (view === viewName);
        el.classList.toggle('active', isActive);
        el.setAttribute('aria-selected', String(isActive));
    });

    // 4. Update active states on Unified Top Navigation Bar
    const topTabs = [
        { el: elements.topNavDashboard, view: 'dashboard' },
        { el: elements.topNavTasks, view: 'tasks' },
        { el: elements.topNavCalendar, view: 'calendar' },
        { el: elements.topNavNotes, view: 'notes' },
        { el: elements.topNavProductivity, view: 'productivity' }
    ];
    topTabs.forEach(({ el, view }) => {
        if (!el) return;
        const isActive = (view === viewName);
        el.classList.toggle('active', isActive);
        el.setAttribute('aria-selected', String(isActive));
    });

    // 5. Update active states on View Switcher buttons
    if (elements.viewBoardBtn) {
        elements.viewBoardBtn.classList.toggle('active', viewName === 'tasks');
    }
    if (elements.viewCalendarBtn) {
        elements.viewCalendarBtn.classList.toggle('active', viewName === 'calendar');
    }

    // 6. Update Header Page Title and Subtitle
    if (elements.pageTitle && elements.pageSubtitle) {
        if (viewName === 'dashboard') {
            elements.pageTitle.textContent = 'Productivity Dashboard';
            elements.pageSubtitle.textContent = 'Workspace overview, task statistics, completion progress, and deadline alerts';
        } else if (viewName === 'tasks') {
            elements.pageTitle.textContent = 'My Tasks';
            elements.pageSubtitle.textContent = 'Interactive Kanban drag-and-drop workspace with search and status columns';
        } else if (viewName === 'calendar') {
            elements.pageTitle.textContent = 'Calendar';
            elements.pageSubtitle.textContent = 'Scheduled deadlines and month calendar view';
        } else if (viewName === 'notes') {
            elements.pageTitle.textContent = 'My Notes';
            elements.pageSubtitle.textContent = 'Quick thoughts, brainstorms, and personal reminders';
        } else if (viewName === 'productivity') {
            elements.pageTitle.textContent = 'Monthly Productivity Tracker';
            elements.pageSubtitle.textContent = 'Performance analytics, daily completion momentum, and month-over-month workload insights';
        }
    }

    // 7. Render view-specific content
    if (viewName === 'dashboard') {
        renderDashboardSnapshot();
    } else if (viewName === 'tasks') {
        applyFiltersAndRender();
    } else if (viewName === 'calendar') {
        renderCalendar();
    } else if (viewName === 'notes') {
        loadNotes();
    } else if (viewName === 'productivity') {
        loadProductivity();
    }

    // Scroll to top of main content
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * Extracts route view from URL hash (e.g., #tasks -> 'tasks')
 */
function getRouteFromHash() {
    const hash = window.location.hash.replace('#', '').trim().toLowerCase();
    if (hash === 'tasks' || hash === 'board' || hash === 'mytasks') return 'tasks';
    if (hash === 'calendar') return 'calendar';
    if (hash === 'notes' || hash === 'stickynotes') return 'notes';
    if (hash === 'productivity' || hash === 'tracker' || hash === 'analytics') return 'productivity';
    return 'dashboard';
}

/**
 * Backward compatibility alias for switchView
 */
function switchView(viewName) {
    if (viewName === 'board') viewName = 'tasks';
    navigateTo(viewName);
}

/**
 * Renders the Active Tasks Quick List and Upcoming Deadlines Snapshot on Dashboard
 */
function renderDashboardSnapshot() {
    if (!elements.dashboardActiveTasksList || !elements.dashboardUpcomingDeadlinesList) return;

    // 1. Active Tasks (Pending and In Progress)
    const activeTasks = state.tasks.filter(t => t.status !== 'Completed');
    if (activeTasks.length === 0) {
        elements.dashboardActiveTasksList.innerHTML = '<div class="widget-empty-hint">🎉 All tasks completed! Great productivity!</div>';
    } else {
        const displayTasks = activeTasks.slice(0, 4);
        elements.dashboardActiveTasksList.innerHTML = displayTasks.map(task => {
            const statusClass = task.status === 'In Progress' ? 'pill-inprogress' : 'pill-pending';
            const prioClass = `badge-priority-${(task.priority || 'medium').toLowerCase()}`;
            return `
                <div class="widget-task-item" onclick="openEditTaskModal(${task.taskId})" role="button" tabindex="0" title="Click to view/edit task details">
                    <div class="widget-task-left">
                        <span class="filter-dot ${task.status === 'In Progress' ? 'inprogress-dot' : 'pending-dot'}"></span>
                        <span class="widget-task-title">${escapeHtml(task.title)}</span>
                    </div>
                    <div class="widget-task-right">
                        <span class="badge ${prioClass}">${escapeHtml(task.priority || 'Med')}</span>
                        <span class="task-status-pill ${statusClass}" style="font-size:0.74rem; padding: 2px 8px;">${escapeHtml(task.status)}</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    // 2. Upcoming Deadlines Snapshot
    const tasksWithDeadlines = state.tasks.filter(t => t.dueDate && t.status !== 'Completed');
    tasksWithDeadlines.sort((a, b) => a.dueDate.localeCompare(b.dueDate));

    if (tasksWithDeadlines.length === 0) {
        elements.dashboardUpcomingDeadlinesList.innerHTML = '<div class="widget-empty-hint">No upcoming deadlines scheduled.</div>';
    } else {
        const displayDeadlines = tasksWithDeadlines.slice(0, 4);
        elements.dashboardUpcomingDeadlinesList.innerHTML = displayDeadlines.map(task => {
            const badgeText = getDueBadgeText(task.dueDate);
            const isOverdue = isTaskOverdue(task);
            const icon = isOverdue ? '⚠️' : '⏰';
            const dateColor = isOverdue ? 'color: #f87171;' : 'color: #fbbf24;';
            return `
                <div class="widget-task-item" onclick="openEditTaskModal(${task.taskId})" role="button" tabindex="0" title="Click to view/edit task details">
                    <div class="widget-task-left">
                        <span style="font-size: 1rem;">${icon}</span>
                        <span class="widget-task-title">${escapeHtml(task.title)}</span>
                    </div>
                    <div class="widget-task-right">
                        <span style="font-size: 0.78rem; font-weight: 600; ${dateColor}">${escapeHtml(badgeText || formatDate(task.dueDate))}</span>
                    </div>
                </div>
            `;
        }).join('');
    }
}

/**
 * Checks MySQL Database connection status via HealthServlet
 */
async function checkHealth() {
    try {
        const response = await fetch(API.HEALTH);
        const data = await response.json();
        
        const dot = elements.dbStatusBadge.querySelector('.status-indicator-dot');
        dot.className = 'status-indicator-dot';

        if (data.database && data.database.connected) {
            dot.classList.add('connected');
            elements.dbStatusText.textContent = 'MySQL Connected';
            elements.dbStatusBadge.title = data.database.message;
        } else {
            dot.classList.add('error');
            elements.dbStatusText.textContent = 'DB Disconnected';
            elements.dbStatusBadge.title = data.database ? data.database.message : 'Connection failed';
        }
    } catch (err) {
        const dot = elements.dbStatusBadge.querySelector('.status-indicator-dot');
        dot.className = 'status-indicator-dot error';
        elements.dbStatusText.textContent = 'Server Offline';
    }
}

/**
 * Loads tasks and statistics from backend
 */
async function loadDashboard() {
    elements.loadingIndicator.style.display = 'block';
    elements.emptyState.style.display = 'none';
    elements.tasksGrid.innerHTML = '';

    try {
        // Fetch tasks and stats in parallel
        const [tasksRes, statsRes] = await Promise.all([
            fetch(API.TASKS),
            fetch(API.DASHBOARD)
        ]);

        const tasksData = await tasksRes.json();
        const statsData = await statsRes.json();

        if (tasksData.success) {
            state.tasks = tasksData.tasks || [];
            renderDeadlineAlerts(state.tasks);
            renderDashboardSnapshot();
            if (state.currentView === 'calendar') {
                renderCalendar();
            } else if (state.currentView === 'tasks') {
                applyFiltersAndRender();
            }
        } else {
            showToast(tasksData.message || 'Failed to fetch tasks', 'error');
        }

        if (statsData.success && statsData.stats) {
            updateStatsUI(statsData.stats);
        }
    } catch (err) {
        console.error('Error fetching dashboard data:', err);
        showToast('Unable to reach server. Check Tomcat or database connection.', 'error');
    } finally {
        elements.loadingIndicator.style.display = 'none';
        if (isInitialLoad) {
            isInitialLoad = false;
            dismissAppLoader();
        }
    }
}

/**
 * Updates Dashboard Summary Stats
 */
function updateStatsUI(stats) {
    const total = stats.total ?? 0;
    const completed = stats.completed ?? 0;
    const pending = stats.pending ?? 0;
    const inProgress = stats.inProgress ?? 0;

    animateValue(elements.statTotal, parseInt(elements.statTotal.textContent) || 0, total, 300);
    animateValue(elements.statPending, parseInt(elements.statPending.textContent) || 0, pending, 300);
    animateValue(elements.statInProgress, parseInt(elements.statInProgress.textContent) || 0, inProgress, 300);
    animateValue(elements.statCompleted, parseInt(elements.statCompleted.textContent) || 0, completed, 300);

    // Update Total Earned Points
    const totalPoints = stats.totalPoints ?? 0;
    state.totalPoints = totalPoints;
    if (elements.statPoints) {
        animateCounter(elements.statPoints, parseInt(elements.statPoints.textContent) || 0, totalPoints, 400);
    }
    if (elements.statPointsSubtext) {
        elements.statPointsSubtext.textContent = totalPoints === 1 ? '1 point earned' : `${totalPoints} points earned`;
    }

    // Calculate completion percentage: Progress = (Completed / Total) * 100
    const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

    if (elements.completionRateText) {
        elements.completionRateText.textContent = `${percent}% completed (${completed}/${total})`;
    }

    // Update Overall Progress Bar Section
    updateProgressUI(total, completed, percent);
}

/**
 * Updates Overall Task Completion Progress Bar and Indicators
 */
function updateProgressUI(total, completed, percent) {
    if (elements.progressTotalCount) elements.progressTotalCount.textContent = total;
    if (elements.progressCompletedCount) elements.progressCompletedCount.textContent = completed;

    if (elements.progressPercentValue) {
        animateValue(elements.progressPercentValue, parseInt(elements.progressPercentValue.textContent) || 0, percent, 350, '%');
    }

    if (elements.progressBarFill) {
        elements.progressBarFill.style.width = `${percent}%`;
    }

    if (elements.progressTrack) {
        elements.progressTrack.setAttribute('aria-valuenow', percent);
    }

    if (elements.progressRemainingText) {
        const remaining = Math.max(0, total - completed);
        elements.progressRemainingText.textContent = `${remaining} task${remaining === 1 ? '' : 's'} remaining`;
    }

    if (elements.progressStatusMessage) {
        if (total === 0) {
            elements.progressStatusMessage.textContent = 'No tasks yet';
        } else if (percent === 100) {
            elements.progressStatusMessage.textContent = '🎉 All tasks completed!';
        } else if (percent >= 75) {
            elements.progressStatusMessage.textContent = 'Almost finished!';
        } else if (percent >= 50) {
            elements.progressStatusMessage.textContent = 'Over halfway done!';
        } else if (percent > 0) {
            elements.progressStatusMessage.textContent = 'Progress underway';
        } else {
            elements.progressStatusMessage.textContent = 'Ready to begin';
        }
    }
}

/**
 * Animates a numeric counter smoothly with optional suffix
 */
function animateValue(obj, start, end, duration, suffix = '') {
    if (!obj) return;
    if (isNaN(start)) start = 0;
    if (start === end) {
        obj.textContent = end + suffix;
        return;
    }
    const range = end - start;
    let current = start;
    const increment = end > start ? 1 : -1;
    const stepTime = Math.abs(Math.floor(duration / (range === 0 ? 1 : range))) || 40;
    const timer = setInterval(() => {
        current += increment;
        obj.textContent = current + suffix;
        if (current === end) {
            clearInterval(timer);
        }
    }, Math.min(stepTime, 50));
}

/**
 * Highlights active filter card
 */
function highlightActiveFilterCard(filterVal) {
    document.querySelectorAll('[data-dashboard-filter]').forEach(card => {
        if (card.getAttribute('data-dashboard-filter') === filterVal) {
            card.classList.add('active-filter');
        } else {
            card.classList.remove('active-filter');
        }
    });
}

/**
 * Clears the active search query and restores all tasks
 */
function clearSearch() {
    elements.searchInput.value = '';
    state.searchQuery = '';
    elements.clearSearchBtn.style.display = 'none';
    elements.searchInput.focus();
    applyFiltersAndRender();
}

/**
 * Filters state.tasks based on active search, status, priority, and category
 */
function getFilteredTasks() {
    return state.tasks.filter(task => {
        // Search filter: case-insensitive match on task title
        if (state.searchQuery) {
            const titleMatch = task.title && task.title.toLowerCase().includes(state.searchQuery);
            if (!titleMatch) return false;
        }

        // Status filter
        if (state.filterStatus !== 'all' && task.status !== state.filterStatus) {
            return false;
        }

        // Priority filter
        if (state.filterPriority !== 'all' && task.priority !== state.filterPriority) {
            return false;
        }

        // Category filter
        if (state.filterCategory !== 'all' && task.category !== state.filterCategory) {
            return false;
        }

        return true;
    });
}


/**
 * Applies client-side search and filters, then renders task cards or calendar
 */
function applyFiltersAndRender() {
    const filtered = getFilteredTasks();
    if (state.currentView === 'calendar') {
        renderCalendar(filtered);
    } else {
        renderTasks(filtered);
    }
}

/**
 * Renders the Calendar View with days of the month and task pills
 */
function renderCalendar(tasksToRender) {
    if (!elements.calendarView || !elements.calendarDaysGrid) return;

    if (elements.tasksBoard) elements.tasksBoard.style.display = 'none';
    if (elements.emptyState) elements.emptyState.style.display = 'none';
    elements.calendarView.style.display = 'flex';

    if (!tasksToRender) {
        tasksToRender = getFilteredTasks();
    }

    const monthNames = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const year = state.calendarYear;
    const month = state.calendarMonth;

    if (elements.calMonthYearTitle) {
        elements.calMonthYearTitle.textContent = `${monthNames[month]} ${year}`;
    }

    // Date calculations: 100% accurate standard JS leap-year and month boundary handling
    const firstDayIndex = new Date(year, month, 1).getDay(); // 0 = Sunday
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    // Map scheduled tasks by YYYY-MM-DD
    const tasksByDate = new Map();
    const unscheduledTasks = [];

    tasksToRender.forEach(task => {
        if (task.dueDate && String(task.dueDate).trim() !== '') {
            const dueStr = String(task.dueDate).split('T')[0].trim();
            if (!tasksByDate.has(dueStr)) {
                tasksByDate.set(dueStr, []);
            }
            tasksByDate.get(dueStr).push(task);
        } else {
            unscheduledTasks.push(task);
        }
    });

    // Today's date info for visual highlight
    const now = new Date();
    const todayY = now.getFullYear();
    const todayM = now.getMonth();
    const todayD = now.getDate();

    elements.calendarDaysGrid.innerHTML = '';
    let scheduledInMonthCount = 0;

    const formatYMD = (y, m, d) => `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

    // 1. Previous Month Padding Days
    const prevMonthY = month === 0 ? year - 1 : year;
    const prevMonthM = month === 0 ? 11 : month - 1;
    for (let i = firstDayIndex - 1; i >= 0; i--) {
        const pDay = daysInPrevMonth - i;
        const pDateStr = formatYMD(prevMonthY, prevMonthM, pDay);
        const dayTasks = tasksByDate.get(pDateStr) || [];

        const cell = createCalendarDayCell({
            dayNumber: pDay,
            dateStr: pDateStr,
            isOtherMonth: true,
            isToday: false,
            tasks: dayTasks
        });
        elements.calendarDaysGrid.appendChild(cell);
    }

    // 2. Current Month Days
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = formatYMD(year, month, day);
        const isToday = (year === todayY && month === todayM && day === todayD);
        const dayTasks = tasksByDate.get(dateStr) || [];
        scheduledInMonthCount += dayTasks.length;

        const cell = createCalendarDayCell({
            dayNumber: day,
            dateStr: dateStr,
            isOtherMonth: false,
            isToday: isToday,
            tasks: dayTasks
        });
        elements.calendarDaysGrid.appendChild(cell);
    }

    // 3. Next Month Padding Days
    const totalCells = firstDayIndex + daysInMonth;
    const targetGridSize = totalCells <= 35 ? 35 : 42;
    const nextMonthPadding = targetGridSize - totalCells;
    const nextMonthY = month === 11 ? year + 1 : year;
    const nextMonthM = month === 11 ? 0 : month + 1;

    for (let nDay = 1; nDay <= nextMonthPadding; nDay++) {
        const nDateStr = formatYMD(nextMonthY, nextMonthM, nDay);
        const dayTasks = tasksByDate.get(nDateStr) || [];

        const cell = createCalendarDayCell({
            dayNumber: nDay,
            dateStr: nDateStr,
            isOtherMonth: true,
            isToday: false,
            tasks: dayTasks
        });
        elements.calendarDaysGrid.appendChild(cell);
    }

    // Update scheduled counter badge
    if (elements.calScheduledBadge) {
        elements.calScheduledBadge.textContent = `${scheduledInMonthCount} scheduled`;
    }

    // Update counter text with search context
    if (state.searchQuery) {
        elements.taskCounterText.textContent = `Calendar: Showing ${tasksToRender.length} task${tasksToRender.length === 1 ? '' : 's'} matching "${state.searchQuery}"`;
    } else {
        elements.taskCounterText.textContent = `Calendar: ${scheduledInMonthCount} task${scheduledInMonthCount === 1 ? '' : 's'} scheduled in ${monthNames[month]} ${year}`;
    }

    // Render Unscheduled Tasks Drawer
    renderUnscheduledDrawer(unscheduledTasks);
}

/**
 * Creates a DOM cell element for a single calendar day
 */
function createCalendarDayCell({ dayNumber, dateStr, isOtherMonth, isToday, tasks }) {
    const cell = document.createElement('div');
    const cellClasses = ['cal-day-cell'];
    if (isOtherMonth) cellClasses.push('other-month');
    if (isToday) cellClasses.push('is-today');
    cell.className = cellClasses.join(' ');
    cell.setAttribute('data-date', dateStr);
    cell.setAttribute('role', 'gridcell');

    // Clicking empty space on a day opens task modal with that date pre-filled
    cell.addEventListener('click', (e) => {
        if (e.target.closest('.cal-task-pill')) return;
        openTaskModalWithDate(dateStr);
    });

    const dayHeader = document.createElement('div');
    dayHeader.className = 'cal-day-header';

    const dayNum = document.createElement('span');
    dayNum.className = 'cal-day-num';
    dayNum.textContent = dayNumber;
    dayHeader.appendChild(dayNum);

    if (tasks.length > 0) {
        const badge = document.createElement('span');
        badge.className = 'cal-day-badge';
        badge.textContent = `${tasks.length} task${tasks.length > 1 ? 's' : ''}`;
        dayHeader.appendChild(badge);
    }

    cell.appendChild(dayHeader);

    const tasksContainer = document.createElement('div');
    tasksContainer.className = 'cal-day-tasks';

    tasks.forEach(task => {
        const pill = document.createElement('div');
        const statusClass = `status-${(task.status || 'pending').toLowerCase().replace(/\s+/g, '')}`;
        pill.className = `cal-task-pill ${statusClass}`;
        pill.setAttribute('data-task-id', task.taskId);
        pill.setAttribute('title', `${task.title}\nStatus: ${task.status}\nPriority: ${task.priority}\nDue Date: ${task.dueDate}`);

        const prioClass = (task.priority || 'medium').toLowerCase();
        const displayTitle = highlightMatch(task.title, state.searchQuery);

        pill.innerHTML = `
            <span class="cal-prio-dot ${prioClass}"></span>
            <span class="cal-task-pill-title">${displayTitle}</span>
        `;

        // Clicking pill opens the task edit modal
        pill.addEventListener('click', (e) => {
            e.stopPropagation();
            openEditTaskModal(task.taskId);
        });

        tasksContainer.appendChild(pill);
    });

    cell.appendChild(tasksContainer);
    return cell;
}

/**
 * Renders tasks that have no due date into the Unscheduled Tasks Drawer
 */
function renderUnscheduledDrawer(unscheduledTasks) {
    if (!elements.unscheduledDrawer || !elements.unscheduledItems) return;

    if (elements.unscheduledCount) {
        elements.unscheduledCount.textContent = unscheduledTasks.length;
    }

    elements.unscheduledItems.innerHTML = '';

    if (unscheduledTasks.length === 0) {
        elements.unscheduledItems.innerHTML = '<span style="color: var(--text-muted); font-size: 0.8rem; font-style: italic;">All current tasks have scheduled due dates.</span>';
        return;
    }

    unscheduledTasks.forEach(task => {
        const pill = document.createElement('button');
        pill.type = 'button';
        pill.className = 'unscheduled-pill';
        const statusDotClass = task.status === 'Completed' ? 'completed-dot' : (task.status === 'In Progress' ? 'inprogress-dot' : 'pending-dot');
        const displayTitle = highlightMatch(task.title, state.searchQuery);

        pill.innerHTML = `
            <span class="filter-dot ${statusDotClass}"></span>
            <span>${displayTitle}</span>
            <span style="opacity: 0.6; font-size: 0.72rem;">(+ Set Due Date)</span>
        `;

        pill.title = `Click to edit "${task.title}" and set a due date`;
        pill.addEventListener('click', () => openEditTaskModal(task.taskId));
        elements.unscheduledItems.appendChild(pill);
    });
}

/**
 * Open task modal with a pre-filled due date
 */
function openTaskModalWithDate(dateStr) {
    openTaskModal();
    if (elements.formDueDate && dateStr) {
        elements.formDueDate.value = dateStr;
    }
}

/**
 * Renders task cards into the Kanban 3-Column Board
 */
function renderTasks(tasks) {
    if (elements.tasksGrid) elements.tasksGrid.innerHTML = '';
    
    // Update counter text with search context
    if (state.searchQuery) {
        elements.taskCounterText.textContent = `Showing ${tasks.length} of ${state.tasks.length} tasks matching "${state.searchQuery}"`;
    } else {
        elements.taskCounterText.textContent = `Showing ${tasks.length} of ${state.tasks.length} tasks`;
    }

    if (tasks.length === 0) {
        if (elements.tasksBoard) elements.tasksBoard.style.display = 'none';
        elements.emptyState.style.display = 'block';
        if (state.searchQuery) {
            // Friendly message when search yields no matches
            if (elements.emptyTitle) elements.emptyTitle.textContent = `No tasks matching "${state.searchQuery}"`;
            if (elements.emptyMessage) {
                elements.emptyMessage.textContent = 'We couldn\'t find any tasks with that title. Check for typos or clear your search to view all tasks.';
            }
            if (elements.emptyActionBtn) {
                elements.emptyActionBtn.textContent = 'Clear Search';
                elements.emptyActionBtn.className = 'btn btn-secondary';
            }
        } else {
            // Standard empty workspace message
            if (elements.emptyTitle) elements.emptyTitle.textContent = 'No Tasks Found';
            if (elements.emptyMessage) {
                elements.emptyMessage.textContent = 'You don\'t have any tasks matching your current view or filter.';
            }
            if (elements.emptyActionBtn) {
                elements.emptyActionBtn.textContent = '+ Create First Task';
                elements.emptyActionBtn.className = 'btn btn-primary';
            }
        }
        if (elements.countPending) elements.countPending.textContent = '0';
        if (elements.countInProgress) elements.countInProgress.textContent = '0';
        if (elements.countCompleted) elements.countCompleted.textContent = '0';
        return;
    }

    elements.emptyState.style.display = 'none';
    if (elements.tasksBoard) elements.tasksBoard.style.display = 'grid';

    // Separate tasks into their respective status columns
    const pendingTasks = tasks.filter(t => t.status === 'Pending');
    const inProgressTasks = tasks.filter(t => t.status === 'In Progress');
    const completedTasks = tasks.filter(t => t.status === 'Completed');

    // Update column count badges
    if (elements.countPending) elements.countPending.textContent = pendingTasks.length;
    if (elements.countInProgress) elements.countInProgress.textContent = inProgressTasks.length;
    if (elements.countCompleted) elements.countCompleted.textContent = completedTasks.length;

    // Render cards into dropzones
    renderColumnDropzone(elements.dropzonePending, pendingTasks, 'Pending');
    renderColumnDropzone(elements.dropzoneInProgress, inProgressTasks, 'In Progress');
    renderColumnDropzone(elements.dropzoneCompleted, completedTasks, 'Completed');
}

/**
 * Renders a specific Kanban column dropzone
 */
function renderColumnDropzone(dropzoneEl, columnTasks, statusName) {
    if (!dropzoneEl) return;
    dropzoneEl.innerHTML = '';

    if (columnTasks.length === 0) {
        const placeholder = document.createElement('div');
        placeholder.className = 'column-empty-placeholder';
        placeholder.textContent = `No ${statusName.toLowerCase()} tasks. Drag tasks here.`;
        dropzoneEl.appendChild(placeholder);
        return;
    }

    columnTasks.forEach(task => {
        const card = createTaskCardElement(task);
        dropzoneEl.appendChild(card);
    });
}

/**
 * Creates an individual draggable task card element
 */
function createTaskCardElement(task) {
    const card = document.createElement('div');
    const isCompleted = task.status === 'Completed';
    const isOverdue = isTaskOverdue(task);
    const isUpcoming = isTaskUpcoming(task);

    let cardClasses = ['task-card'];
    if (isCompleted) cardClasses.push('completed');
    if (isOverdue) cardClasses.push('overdue');
    if (isUpcoming) cardClasses.push('due-soon');
    card.className = cardClasses.join(' ');
    card.setAttribute('data-id', task.taskId);
    card.setAttribute('draggable', 'true');

    // HTML5 Drag Events for Card
    card.addEventListener('dragstart', (e) => {
        state.draggedTaskId = task.taskId;
        state.draggedFromStatus = task.status;
        e.dataTransfer.setData('text/plain', String(task.taskId));
        e.dataTransfer.effectAllowed = 'move';
        setTimeout(() => card.classList.add('dragging'), 0);
    });

    card.addEventListener('dragend', () => {
        card.classList.remove('dragging');
        state.draggedTaskId = null;
        state.draggedFromStatus = null;
        document.querySelectorAll('.kanban-dropzone').forEach(dz => dz.classList.remove('drag-over'));
    });

    const priorityClass = `badge-priority-${(task.priority || 'medium').toLowerCase()}`;
    const formattedDueDate = task.dueDate ? formatDate(task.dueDate) : 'No due date';
    const displayTitle = highlightMatch(task.title, state.searchQuery);

    // Deadline badges and footer styling
    let deadlineBadgeHtml = '';
    let dueDateClass = '';
    let dueDateText = formattedDueDate;

    if (isOverdue) {
        deadlineBadgeHtml = `<span class="badge badge-overdue" title="Due date has passed">⚠️ Overdue</span>`;
        dueDateClass = 'overdue-date';
        dueDateText = `⚠️ Overdue (${formattedDueDate})`;
    } else if (isUpcoming) {
        const badgeText = getDueBadgeText(task.dueDate);
        deadlineBadgeHtml = `<span class="badge badge-due-soon" title="${badgeText}">⏰ ${badgeText}</span>`;
        dueDateClass = 'due-soon-date';
        dueDateText = `⏰ Due ${formattedDueDate}`;
    }

    card.innerHTML = `
        <div>
            <div class="task-header">
                <div class="task-meta">
                    <span class="badge ${priorityClass}">${escapeHtml(task.priority || 'Medium')}</span>
                    <span class="badge badge-category">${escapeHtml(task.category || 'General')}</span>
                    ${deadlineBadgeHtml}
                </div>
                <div class="task-actions-menu">
                    <button class="icon-btn edit-btn" title="Edit Task" onclick="openEditTaskModal(${task.taskId})">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                    </button>
                    <button class="icon-btn delete-btn" title="Delete Task" onclick="promptDeleteTask(${task.taskId}, '${escapeJsString(task.title)}')">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                    </button>
                </div>
            </div>

            <h4 class="task-title">${displayTitle}</h4>
            <p class="task-description">${escapeHtml(task.description || 'No description provided.')}</p>
        </div>

        <div class="task-footer">
            <div class="task-due-date ${dueDateClass}" title="Due Date">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="16" y1="2" x2="16" y2="6"></line>
                    <line x1="8" y1="2" x2="8" y2="6"></line>
                    <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
                <span>${dueDateText}</span>
            </div>

            <!-- Fallback Status Selector for Accessible / Non-Drag Devices -->
            <div class="status-selector">
                <select class="task-status-pill ${getStatusPillClass(task.status)}" 
                        onchange="updateTaskStatusDirectly(${task.taskId}, this.value)" 
                        title="Change Status"
                        aria-label="Change status for ${escapeHtml(task.title)}">
                    <option value="Pending" ${task.status === 'Pending' ? 'selected' : ''}>Pending</option>
                    <option value="In Progress" ${task.status === 'In Progress' ? 'selected' : ''}>In Progress</option>
                    <option value="Completed" ${task.status === 'Completed' ? 'selected' : ''}>Completed</option>
                </select>
            </div>
        </div>
    `;

    return card;
}

/**
 * Initializes HTML5 Drag and Drop on Kanban Dropzones
 */
function initKanbanDropzones() {
    const zones = [
        { el: elements.dropzonePending, status: 'Pending' },
        { el: elements.dropzoneInProgress, status: 'In Progress' },
        { el: elements.dropzoneCompleted, status: 'Completed' }
    ];

    zones.forEach(({ el, status }) => {
        if (!el) return;

        el.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            el.classList.add('drag-over');
        });

        el.addEventListener('dragleave', (e) => {
            if (e.relatedTarget && el.contains(e.relatedTarget)) return;
            el.classList.remove('drag-over');
        });

        el.addEventListener('drop', (e) => {
            e.preventDefault();
            el.classList.remove('drag-over');
            const taskIdStr = e.dataTransfer.getData('text/plain') || state.draggedTaskId;
            const taskId = Number(taskIdStr);
            handleTaskDrop(taskId, status, state.draggedFromStatus);
        });
    });
}

/**
 * Handles Task Drop with MySQL Persistence and Rollback on Failure
 */
async function handleTaskDrop(taskId, targetStatus, originalStatus) {
    if (!taskId || !targetStatus) return;
    if (targetStatus === originalStatus) return; // Dropped in the same column, no update needed

    const task = state.tasks.find(t => t.taskId === taskId);
    if (!task) return;

    const previousStatus = task.status;

    const params = new URLSearchParams();
    params.append('action', 'status');
    params.append('taskId', taskId);
    params.append('status', targetStatus);

    try {
        const response = await fetch(API.TASKS, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await response.json();
        if (data.success) {
            // Update successful in MySQL! Move card and refresh
            task.status = targetStatus;
            state.dismissedAlerts.overdue = false;
            state.dismissedAlerts.upcoming = false;
            if (data.pointsAwarded && data.pointsAwarded > 0) {
                triggerPointsCelebration(data.pointsAwarded, data.totalPoints, task.title);
            } else {
                showToast(`Moved "${task.title}" to ${targetStatus}`, 'success');
            }
            // Re-fetch dashboard to update progress bar, alerts, counters, and total points
            loadDashboard();
        } else {
            // Rollback: Keep in original column and notify user
            task.status = previousStatus;
            showToast(data.message || 'Failed to move task. Reverted.', 'error');
            applyFiltersAndRender();
        }
    } catch (err) {
        console.error('Drag and drop status update error:', err);
        task.status = previousStatus;
        showToast('Network error while moving task. Card restored.', 'error');
        applyFiltersAndRender();
    }
}

function getStatusPillClass(status) {
    if (status === 'Completed') return 'pill-completed';
    if (status === 'In Progress') return 'pill-inprogress';
    return 'pill-pending';
}

/**
 * Open Modal for Creating Task
 */
function openTaskModal() {
    elements.modalTitle.textContent = 'Create New Task';
    elements.formTaskId.value = '';
    elements.taskForm.reset();
    elements.titleError.textContent = '';
    elements.formPriority.value = 'Medium';
    elements.formStatus.value = 'Pending';
    elements.formCategory.value = 'College';
    elements.taskModal.style.display = 'flex';
    elements.formTitle.focus();
}

/**
 * Open Modal for Editing Task
 */
window.openEditTaskModal = function(taskId) {
    const task = state.tasks.find(t => t.taskId === taskId);
    if (!task) return;

    elements.modalTitle.textContent = 'Edit Task';
    elements.formTaskId.value = task.taskId;
    elements.formTitle.value = task.title || '';
    elements.formDescription.value = task.description || '';
    elements.formPriority.value = task.priority || 'Medium';
    elements.formStatus.value = task.status || 'Pending';
    elements.formCategory.value = task.category || 'College';
    elements.formDueDate.value = task.dueDate || '';
    elements.titleError.textContent = '';

    elements.taskModal.style.display = 'flex';
    elements.formTitle.focus();
};

function closeTaskModal() {
    elements.taskModal.style.display = 'none';
}

/**
 * Handle Task Form Submission (Create or Update)
 */
async function handleFormSubmit(e) {
    e.preventDefault();

    const taskId = elements.formTaskId.value.trim();
    const title = elements.formTitle.value.trim();
    const description = elements.formDescription.value.trim();
    const priority = elements.formPriority.value;
    const status = elements.formStatus.value;
    const category = elements.formCategory.value;
    const dueDate = elements.formDueDate.value;

    // Validation
    if (!title) {
        elements.titleError.textContent = 'Please enter a task title.';
        elements.formTitle.focus();
        return;
    }
    elements.titleError.textContent = '';

    const isEdit = Boolean(taskId);
    const params = new URLSearchParams();
    params.append('action', isEdit ? 'update' : 'create');
    if (isEdit) params.append('taskId', taskId);
    params.append('title', title);
    params.append('description', description);
    params.append('priority', priority);
    params.append('status', status);
    params.append('category', category);
    if (dueDate) params.append('dueDate', dueDate);

    if (elements.saveTaskBtn) elements.saveTaskBtn.disabled = true;

    try {
        const response = await fetch(API.TASKS, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await response.json();
        if (data.success) {
            state.dismissedAlerts.overdue = false;
            state.dismissedAlerts.upcoming = false;
            if (data.pointsAwarded && data.pointsAwarded > 0) {
                triggerPointsCelebration(data.pointsAwarded, data.totalPoints, title);
            } else {
                showToast(isEdit ? 'Task updated successfully!' : 'Task created successfully!', 'success');
            }
            closeTaskModal();
            loadDashboard();
        } else {
            showToast(data.message || 'Error saving task', 'error');
        }
    } catch (err) {
        console.error('Save task error:', err);
        showToast('Error connecting to server. Please try again.', 'error');
    } finally {
        if (elements.saveTaskBtn) elements.saveTaskBtn.disabled = false;
    }
}

/**
 * Direct Status Update from Dropdown
 */
const inFlightStatusUpdates = new Set();

window.updateTaskStatusDirectly = async function(taskId, newStatus) {
    if (inFlightStatusUpdates.has(taskId)) return;
    inFlightStatusUpdates.add(taskId);

    const params = new URLSearchParams();
    params.append('action', 'status');
    params.append('taskId', taskId);
    params.append('status', newStatus);

    try {
        const response = await fetch(API.TASKS, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await response.json();
        if (data.success) {
            if (data.pointsAwarded && data.pointsAwarded > 0) {
                const targetTask = state.tasks.find(t => t.taskId === taskId);
                triggerPointsCelebration(data.pointsAwarded, data.totalPoints, targetTask ? targetTask.title : '');
            } else {
                showToast(`Task status updated to ${newStatus}`, 'success');
            }
            loadDashboard();
        } else {
            showToast(data.message || 'Status update failed', 'error');
        }
    } catch (err) {
        console.error('Status update error:', err);
        showToast('Network error while updating status.', 'error');
    } finally {
        inFlightStatusUpdates.delete(taskId);
    }
};

/**
 * Delete Confirmation Modal
 */
window.promptDeleteTask = function(taskId, title) {
    state.taskToDeleteId = taskId;
    state.noteToDeleteId = null;
    if (elements.deleteModalTitle) elements.deleteModalTitle.textContent = 'Delete Task?';
    if (elements.deleteModalDesc) {
        elements.deleteModalDesc.innerHTML = `Are you sure you want to delete <strong id="deleteTaskTitle">"${escapeHtml(title)}"</strong>? This action will permanently remove it from MySQL database.`;
    }
    if (elements.confirmDeleteBtnText) elements.confirmDeleteBtnText.textContent = 'Delete Task';
    elements.deleteModal.style.display = 'flex';
};

function closeDeleteModal() {
    elements.deleteModal.style.display = 'none';
    state.taskToDeleteId = null;
    state.noteToDeleteId = null;
    if (elements.deleteModalTitle) elements.deleteModalTitle.textContent = 'Delete Task?';
    if (elements.confirmDeleteBtnText) elements.confirmDeleteBtnText.textContent = 'Delete Task';
}

async function executeDeleteTask() {
    if (state.noteToDeleteId) {
        await executeDeleteNote();
        return;
    }
    if (!state.taskToDeleteId) return;

    const params = new URLSearchParams();
    params.append('action', 'delete');
    params.append('taskId', state.taskToDeleteId);

    try {
        const response = await fetch(API.TASKS, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await response.json();
        if (data.success) {
            showToast('Task permanently deleted from database.', 'success');
            closeDeleteModal();
            loadDashboard();
        } else {
            showToast(data.message || 'Failed to delete task', 'error');
        }
    } catch (err) {
        console.error('Delete task error:', err);
        showToast('Error communicating with server.', 'error');
    }
}

/* ==========================================================================
   POINTS CELEBRATION & MILESTONES SYSTEM
   ========================================================================== */

/**
 * Cumulative Point Milestones
 * Each milestone celebration must happen only once per user, per milestone.
 */
const MILESTONE_DEFINITIONS = [
    {
        threshold: 100,
        badge: '🏆 Century Milestone',
        icon: '🏆',
        title: '100 Points Milestone Reached!',
        message: "Century Club unlocked! You've completed tasks and earned 100 productivity points. Keep up the phenomenal momentum!"
    },
    {
        threshold: 500,
        badge: '🌟 Half-Grand Milestone',
        icon: '🌟',
        title: '500 Points Milestone Reached!',
        message: "Productivity Champion! You've reached 500 points with unstoppable focus and dedication!"
    },
    {
        threshold: 1000,
        badge: '👑 Legend Milestone',
        icon: '👑',
        title: '1,000 Points Milestone Reached!',
        message: "TaskVerse Legend! You've surpassed 1,000 productivity points. True mastery of time and tasks!"
    }
];

/**
 * Lightweight Zero-Dependency Canvas Particle System
 * Emits colorful confetti ribbons, stars, sparkles, and circles.
 */
class CelebrationCanvasSystem {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas ? this.canvas.getContext('2d') : null;
        this.particles = [];
        this.animId = null;
        this.resize();
        window.addEventListener('resize', () => this.resize(), { passive: true });
    }

    resize() {
        if (!this.canvas) return;
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
    }

    burst(options = {}) {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        if (!this.canvas || !this.ctx) return;

        const isMilestone = Boolean(options.isMilestone);
        const count = isMilestone ? 130 : 50;
        const originX = options.x ?? (window.innerWidth / 2);
        const originY = options.y ?? (window.innerHeight * 0.22);

        const colors = [
            '#8B5CF6', // vibrant violet
            '#C4B5FD', // bright lavender
            '#D946EF', // neon magenta
            '#A78BFA', // soft purple
            '#F59E0B', // warm amber
            '#10B981', // emerald
            '#38BDF8', // sky cyan
            '#F43F5E'  // rose
        ];

        const shapes = ['star', 'sparkle', 'ribbon', 'circle'];

        for (let i = 0; i < count; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = isMilestone ? (Math.random() * 8 + 3) : (Math.random() * 5.5 + 2);
            const posX = isMilestone ? (Math.random() * window.innerWidth) : (originX + (Math.random() * 50 - 25));
            const posY = isMilestone ? (Math.random() * (window.innerHeight * 0.4)) : originY;

            this.particles.push({
                x: posX,
                y: posY,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed - (isMilestone ? 3.5 : 4.5),
                size: isMilestone ? (Math.random() * 7 + 4) : (Math.random() * 5 + 3),
                color: colors[Math.floor(Math.random() * colors.length)],
                shape: shapes[Math.floor(Math.random() * shapes.length)],
                rotation: Math.random() * 360,
                rotationSpeed: (Math.random() - 0.5) * 12,
                opacity: 1,
                decay: isMilestone ? (Math.random() * 0.007 + 0.006) : (Math.random() * 0.016 + 0.012),
                gravity: isMilestone ? 0.17 : 0.22,
                drag: 0.985
            });
        }

        if (!this.animId) {
            this.loop();
        }
    }

    loop() {
        if (!this.ctx || this.particles.length === 0) {
            if (this.ctx && this.canvas) this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            this.animId = null;
            return;
        }

        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        for (let i = this.particles.length - 1; i >= 0; i--) {
            const p = this.particles[i];
            p.vx *= p.drag;
            p.vy *= p.drag;
            p.vy += p.gravity;
            p.x += p.vx;
            p.y += p.vy;
            p.rotation += p.rotationSpeed;
            p.opacity -= p.decay;

            if (p.opacity <= 0 || p.y > this.canvas.height + 50) {
                this.particles.splice(i, 1);
                continue;
            }

            this.ctx.save();
            this.ctx.translate(p.x, p.y);
            this.ctx.rotate((p.rotation * Math.PI) / 180);
            this.ctx.globalAlpha = Math.max(0, p.opacity);
            this.ctx.fillStyle = p.color;

            if (p.shape === 'star') {
                this.drawStar(this.ctx, 0, 0, 5, p.size, p.size / 2);
            } else if (p.shape === 'sparkle') {
                this.drawSparkle(this.ctx, 0, 0, p.size);
            } else if (p.shape === 'ribbon') {
                this.ctx.fillRect(-p.size / 2, -p.size, p.size, p.size * 2);
            } else {
                this.ctx.beginPath();
                this.ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
                this.ctx.fill();
            }

            this.ctx.restore();
        }

        this.animId = requestAnimationFrame(() => this.loop());
    }

    drawStar(ctx, cx, cy, spikes, outerRadius, innerRadius) {
        let rot = (Math.PI / 2) * 3;
        let x = cx;
        let y = cy;
        const step = Math.PI / spikes;

        ctx.beginPath();
        ctx.moveTo(cx, cy - outerRadius);
        for (let i = 0; i < spikes; i++) {
            x = cx + Math.cos(rot) * outerRadius;
            y = cy + Math.sin(rot) * outerRadius;
            ctx.lineTo(x, y);
            rot += step;

            x = cx + Math.cos(rot) * innerRadius;
            y = cy + Math.sin(rot) * innerRadius;
            ctx.lineTo(x, y);
            rot += step;
        }
        ctx.lineTo(cx, cy - outerRadius);
        ctx.closePath();
        ctx.fill();
    }

    drawSparkle(ctx, cx, cy, size) {
        ctx.beginPath();
        ctx.moveTo(cx, cy - size);
        ctx.quadraticCurveTo(cx, cy, cx + size, cy);
        ctx.quadraticCurveTo(cx, cy, cx, cy + size);
        ctx.quadraticCurveTo(cx, cy, cx - size, cy);
        ctx.quadraticCurveTo(cx, cy, cx, cy - size);
        ctx.closePath();
        ctx.fill();
    }

    clear() {
        this.particles = [];
        if (this.ctx && this.canvas) this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        if (this.animId) {
            cancelAnimationFrame(this.animId);
            this.animId = null;
        }
    }
}

let celebrationCanvas = null;

function getCelebratedMilestones() {
    try {
        const stored = localStorage.getItem('taskverse_celebrated_milestones');
        if (stored) {
            return new Set(JSON.parse(stored));
        }
    } catch (e) {
        console.warn('Could not read celebrated milestones from localStorage:', e);
    }
    return new Set();
}

function saveCelebratedMilestones(milestonesSet) {
    try {
        localStorage.setItem('taskverse_celebrated_milestones', JSON.stringify([...milestonesSet]));
    } catch (e) {
        console.warn('Could not save celebrated milestones to localStorage:', e);
    }
}

function initCelebrationSystem() {
    celebrationCanvas = new CelebrationCanvasSystem('celebrationCanvas');
    state.celebratedMilestones = getCelebratedMilestones();
}

/**
 * High-framerate counter animator with easeOutCubic and cancellation
 */
function animateCounter(obj, start, end, duration = 750, suffix = '') {
    if (!obj) return;
    if (isNaN(start)) start = 0;
    if (start === end) {
        obj.textContent = end + suffix;
        return;
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        obj.textContent = end + suffix;
        return;
    }

    if (obj._animFrameId) {
        cancelAnimationFrame(obj._animFrameId);
        obj._animFrameId = null;
    }

    const startTime = performance.now();
    const range = end - start;

    function frame(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const ease = 1 - Math.pow(1 - progress, 3);
        const currentVal = Math.round(start + range * ease);
        obj.textContent = currentVal + suffix;

        if (progress < 1) {
            obj._animFrameId = requestAnimationFrame(frame);
        } else {
            obj.textContent = end + suffix;
            obj._animFrameId = null;
        }
    }

    obj._animFrameId = requestAnimationFrame(frame);
}

/**
 * Checks if this points transaction crossed any milestone threshold.
 * Guarantees each milestone celebrates only once per user.
 */
function checkAndTriggerMilestone(previousPoints, newTotalPoints) {
    if (!state.celebratedMilestones) {
        state.celebratedMilestones = getCelebratedMilestones();
    }

    const unlocked = MILESTONE_DEFINITIONS.find(m =>
        previousPoints < m.threshold &&
        newTotalPoints >= m.threshold &&
        !state.celebratedMilestones.has(m.threshold)
    );

    if (unlocked) {
        state.celebratedMilestones.add(unlocked.threshold);
        saveCelebratedMilestones(state.celebratedMilestones);
        showMilestoneCelebration(unlocked, newTotalPoints);
        return unlocked;
    }
    return null;
}

/**
 * Displays Milestone Celebration modal dialog
 */
function showMilestoneCelebration(milestone, totalPoints) {
    const modal = elements.milestoneModal || document.getElementById('milestoneModal');
    if (!modal) return;

    const badgeTop = elements.milestoneBadgeTop || document.getElementById('milestoneBadgeTop');
    const iconEl = elements.milestoneIcon || document.getElementById('milestoneIcon');
    const titleEl = elements.milestoneTitle || document.getElementById('milestoneTitle');
    const msgEl = elements.milestoneMessage || document.getElementById('milestoneMessage');
    const totalEl = elements.milestonePointsTotal || document.getElementById('milestonePointsTotal');

    if (badgeTop) badgeTop.textContent = milestone.badge;
    if (iconEl) iconEl.textContent = milestone.icon;
    if (titleEl) titleEl.textContent = milestone.title;
    if (msgEl) msgEl.textContent = milestone.message;
    if (totalEl) totalEl.textContent = totalPoints;

    modal.style.display = 'flex';

    // Auto-dismiss after 6.5 seconds
    clearTimeout(modal._autoDismissTimer);
    modal._autoDismissTimer = setTimeout(() => {
        closeMilestoneModal();
    }, 6500);
}

function closeMilestoneModal() {
    const modal = elements.milestoneModal || document.getElementById('milestoneModal');
    if (modal) {
        clearTimeout(modal._autoDismissTimer);
        modal.style.display = 'none';
    }
}

/**
 * Creates and animates a floating "+XX Points" notification pill
 */
function showFloatingPointsPill(pointsAwarded, motivationalMsg, taskTitle) {
    const container = elements.celebrationContainer || document.getElementById('celebrationContainer');
    if (!container) return;

    const pill = document.createElement('div');
    pill.className = 'points-float-pill';
    pill.setAttribute('role', 'alert');

    const cleanTitle = taskTitle
        ? `Task: ${escapeHtml(taskTitle.length > 30 ? taskTitle.substring(0, 30) + '...' : taskTitle)}`
        : 'Task Completed';

    pill.innerHTML = `
        <div class="points-pill-badge">
            <span class="sparkle-star">✨</span>
            <span>+${pointsAwarded} Points</span>
        </div>
        <div class="points-pill-content">
            <span class="points-pill-msg">${escapeHtml(motivationalMsg)}</span>
            <span class="points-pill-subtext">${cleanTitle}</span>
        </div>
    `;

    container.appendChild(pill);

    setTimeout(() => {
        if (pill.parentNode) {
            pill.parentNode.removeChild(pill);
        }
    }, 2200);
}

/**
 * Master Points Celebration Trigger
 * Strictly triggered ONLY when points are genuinely awarded.
 */
function triggerPointsCelebration(pointsAwarded, newTotalPoints, taskTitle = '') {
    if (!pointsAwarded || pointsAwarded <= 0) return;

    const currentPointsEl = elements.statPoints;
    const previousPoints = parseInt(currentPointsEl ? currentPointsEl.textContent : '0') || (state.totalPoints ?? 0);

    // Update state total points
    state.totalPoints = newTotalPoints;

    // 1. Animate Points Counter Smoothly
    if (currentPointsEl) {
        animateCounter(currentPointsEl, previousPoints, newTotalPoints, 800);
    }
    if (elements.statPointsSubtext) {
        elements.statPointsSubtext.textContent = newTotalPoints === 1 ? '1 point earned' : `${newTotalPoints} points earned`;
    }

    // 2. Subtle Pop / Bounce Effect on Total Points Card
    const pointsCard = elements.cardTotalPoints || document.getElementById('cardTotalPoints');
    if (pointsCard) {
        pointsCard.classList.remove('points-earned-bounce');
        void pointsCard.offsetWidth; // Force reflow to re-trigger animation
        pointsCard.classList.add('points-earned-bounce');
        setTimeout(() => {
            pointsCard.classList.remove('points-earned-bounce');
        }, 1200);
    }

    // 3. Check for Milestone Unlock (100, 500, 1000)
    const milestone = checkAndTriggerMilestone(previousPoints, newTotalPoints);

    // 4. Random Motivational Message
    const motivationalMessages = [
        "Amazing work!",
        "You're on a roll!",
        "Great job! Keep going!",
        "Outstanding progress!",
        "Productivity unlocked!",
        "Crushing your goals!"
    ];
    const randomMsg = motivationalMessages[Math.floor(Math.random() * motivationalMessages.length)];

    // 5. Display Floating "+XX Points" Pill
    showFloatingPointsPill(pointsAwarded, randomMsg, taskTitle);

    // 6. Confetti & Particle Burst
    if (celebrationCanvas) {
        let burstX = window.innerWidth / 2;
        let burstY = window.innerHeight * 0.22;
        if (pointsCard && pointsCard.offsetParent !== null) {
            const rect = pointsCard.getBoundingClientRect();
            if (rect.top >= 0 && rect.bottom <= window.innerHeight) {
                burstX = rect.left + rect.width / 2;
                burstY = rect.top + rect.height / 2;
            }
        }
        celebrationCanvas.burst({
            x: burstX,
            y: burstY,
            isMilestone: Boolean(milestone)
        });
    }

    // 7. Toast Notification for consistency
    showToast(`🎉 +${pointsAwarded} points earned! Total score: ${newTotalPoints}`, 'success');
}

/**
 * Toast Notification System
 * Includes auto-dismiss, duplicate debouncing, and manual dismiss button.
 */
let lastToastState = { message: '', time: 0 };

function showToast(message, type = 'info') {
    const now = Date.now();
    // Debounce identical duplicate notifications within 1.5 seconds
    if (lastToastState.message === message && (now - lastToastState.time) < 1500) {
        return;
    }
    lastToastState = { message, time: now };

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.setAttribute('role', 'status');
    toast.setAttribute('aria-live', 'polite');
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '✕';
    if (type === 'warning') icon = '⚠️';

    toast.innerHTML = `
        <span class="toast-icon">${icon}</span>
        <span class="toast-msg">${escapeHtml(message)}</span>
        <button class="toast-close-btn" aria-label="Dismiss notification" title="Dismiss">&times;</button>
    `;

    const closeBtn = toast.querySelector('.toast-close-btn');
    let autoDismissTimer;

    const removeToast = () => {
        if (!toast.parentElement) return;
        clearTimeout(autoDismissTimer);
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        toast.style.transition = 'all 0.25s ease';
        setTimeout(() => toast.remove(), 250);
    };

    closeBtn.addEventListener('click', removeToast);

    // Auto dismiss after 4 seconds
    autoDismissTimer = setTimeout(removeToast, 4000);

    // Pause dismissal when hovered
    toast.addEventListener('mouseenter', () => clearTimeout(autoDismissTimer));
    toast.addEventListener('mouseleave', () => {
        autoDismissTimer = setTimeout(removeToast, 2000);
    });

    elements.toastContainer.appendChild(toast);
}

/**
 * Returns today's date formatted as YYYY-MM-DD
 */
function getTodayString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/**
 * Calculates calendar days between today and dueDateStr (YYYY-MM-DD)
 */
function getDaysDifference(dueDateStr) {
    if (!dueDateStr) return null;
    try {
        const [y1, m1, d1] = getTodayString().split('-').map(Number);
        const [y2, m2, d2] = dueDateStr.split('-').map(Number);
        const dToday = new Date(y1, m1 - 1, d1);
        const dDue = new Date(y2, m2 - 1, d2);
        const diffTime = dDue.getTime() - dToday.getTime();
        return Math.round(diffTime / (1000 * 60 * 60 * 24));
    } catch {
        return null;
    }
}

/**
 * Returns true if a task is overdue (past due date & not completed)
 */
function isTaskOverdue(task) {
    if (!task.dueDate || task.status === 'Completed') return false;
    const diff = getDaysDifference(task.dueDate);
    return diff !== null && diff < 0;
}

/**
 * Returns true if a task deadline is today or within the next 3 days & not completed
 */
function isTaskUpcoming(task) {
    if (!task.dueDate || task.status === 'Completed') return false;
    const diff = getDaysDifference(task.dueDate);
    return diff !== null && diff >= 0 && diff <= 3;
}

/**
 * Returns friendly relative deadline text
 */
function getDueBadgeText(dueDateStr) {
    const diff = getDaysDifference(dueDateStr);
    if (diff === null) return '';
    if (diff < 0) {
        const abs = Math.abs(diff);
        return `Overdue (${abs}d)`;
    }
    if (diff === 0) return 'Due Today';
    if (diff === 1) return 'Due Tomorrow';
    return `Due in ${diff} days`;
}

/**
 * Renders Overdue & Upcoming Deadline Alert Banners
 */
function renderDeadlineAlerts(allTasks) {
    if (!elements.alertsContainer) return;

    const overdueTasks = allTasks.filter(t => isTaskOverdue(t));
    const upcomingTasks = allTasks.filter(t => isTaskUpcoming(t));

    let html = '';

    // Overdue Alert Banner
    if (overdueTasks.length > 0 && !state.dismissedAlerts.overdue) {
        const itemsHtml = overdueTasks.map(t => {
            const diff = Math.abs(getDaysDifference(t.dueDate) || 1);
            return `<span class="alert-pill alert-pill-overdue">
                <strong>${escapeHtml(t.title)}</strong> (Due ${formatDate(t.dueDate)} • ${diff}d overdue)
            </span>`;
        }).join('');

        html += `
            <div class="alert-banner alert-overdue" role="alert">
                <div class="alert-icon-wrapper">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                </div>
                <div class="alert-content">
                    <div class="alert-title">
                        <strong>⚠️ Action Required:</strong> You have <span>${overdueTasks.length}</span> overdue task${overdueTasks.length > 1 ? 's' : ''}!
                    </div>
                    <div class="alert-items">
                        ${itemsHtml}
                    </div>
                </div>
                <button class="alert-dismiss-btn" title="Dismiss overdue alert" onclick="dismissOverdueAlert()">&times;</button>
            </div>
        `;
    }

    // Upcoming Deadlines Banner (Due today or within next 3 days)
    if (upcomingTasks.length > 0 && !state.dismissedAlerts.upcoming) {
        const itemsHtml = upcomingTasks.map(t => {
            const badgeText = getDueBadgeText(t.dueDate);
            return `<span class="alert-pill alert-pill-upcoming">
                <strong>${escapeHtml(t.title)}</strong> (${badgeText} • ${formatDate(t.dueDate)})
            </span>`;
        }).join('');

        html += `
            <div class="alert-banner alert-upcoming" role="alert">
                <div class="alert-icon-wrapper">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <circle cx="12" cy="12" r="10"></circle>
                        <polyline points="12 6 12 12 16 14"></polyline>
                    </svg>
                </div>
                <div class="alert-content">
                    <div class="alert-title">
                        <strong>⏰ Upcoming Deadlines:</strong> <span>${upcomingTasks.length}</span> task${upcomingTasks.length > 1 ? 's' : ''} due soon (today or within 3 days):
                    </div>
                    <div class="alert-items">
                        ${itemsHtml}
                    </div>
                </div>
                <button class="alert-dismiss-btn" title="Dismiss upcoming alert" onclick="dismissUpcomingAlert()">&times;</button>
            </div>
        `;
    }

    if (html) {
        elements.alertsContainer.innerHTML = html;
        elements.alertsContainer.style.display = 'flex';
    } else {
        elements.alertsContainer.innerHTML = '';
        elements.alertsContainer.style.display = 'none';
    }
}

/**
 * Handlers to dismiss alert banners for the session
 */
window.dismissOverdueAlert = function() {
    state.dismissedAlerts.overdue = true;
    renderDeadlineAlerts(state.tasks);
};

window.dismissUpcomingAlert = function() {
    state.dismissedAlerts.upcoming = true;
    renderDeadlineAlerts(state.tasks);
};

/**
 * Helper: Formats YYYY-MM-DD into readable date
 */
function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
        const parts = dateStr.split('-');
        if (parts.length === 3) {
            const date = new Date(parts[0], parts[1] - 1, parts[2]);
            return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
        }
        return dateStr;
    } catch {
        return dateStr;
    }
}

/**
 * Helper: Escapes HTML strings to prevent XSS
 */
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Helper: Escapes Javascript string literals
 */
function escapeJsString(str) {
    if (!str) return '';
    return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
}

/**
 * Helper: Escapes characters for regular expressions
 */
function escapeRegex(str) {
    if (!str) return '';
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Helper: Safely highlights matching search query in task title
 */
function highlightMatch(text, query) {
    const escaped = escapeHtml(text || '');
    if (!query) return escaped;
    const escapedQuery = escapeRegex(escapeHtml(query));
    try {
        const regex = new RegExp(`(${escapedQuery})`, 'gi');
        return escaped.replace(regex, '<mark class="search-highlight">$1</mark>');
    } catch {
        return escaped;
    }
}

/**
 * =====================================================================
 * Sticky Notes Module
 * =====================================================================
 */

/**
 * Clears Sticky Notes search query and resets filter input
 */
function clearNotesSearch() {
    if (elements.notesSearchInput) elements.notesSearchInput.value = '';
    state.notesSearchQuery = '';
    if (elements.clearNotesSearchBtn) elements.clearNotesSearchBtn.style.display = 'none';
    renderNotes();
}
window.clearNotesSearch = clearNotesSearch;

/**
 * Loads all notes from MySQL via NoteServlet
 */
async function loadNotes(showLoading = true) {
    if (!elements.notesGrid) return;
    if (showLoading && elements.notesLoadingIndicator) {
        elements.notesLoadingIndicator.style.display = 'flex';
    }

    try {
        const response = await fetch(API.NOTES);
        const data = await response.json();
        if (data.success) {
            state.notes = data.notes || [];
            renderNotes();
        } else {
            showToast(data.message || 'Failed to load notes', 'error');
        }
    } catch (err) {
        console.error('Error loading notes:', err);
        showToast('Unable to reach server to fetch notes.', 'error');
    } finally {
        if (elements.notesLoadingIndicator) {
            elements.notesLoadingIndicator.style.display = 'none';
        }
    }
}

/**
 * Renders Sticky Notes Grid
 */
function renderNotes() {
    if (!elements.notesGrid) return;

    // Filter notes if search query is active
    const q = state.notesSearchQuery;
    const filtered = q
        ? state.notes.filter(n => (n.title && n.title.toLowerCase().includes(q)) || (n.content && n.content.toLowerCase().includes(q)))
        : state.notes;

    // Update note count badge
    if (elements.notesCounterBadge) {
        const count = state.notes.length;
        elements.notesCounterBadge.textContent = `${count} note${count === 1 ? '' : 's'}`;
    }

    // Handle empty state (no notes in database)
    if (state.notes.length === 0) {
        elements.notesGrid.innerHTML = '';
        if (elements.notesEmptyState) elements.notesEmptyState.style.display = 'flex';
        return;
    }

    if (elements.notesEmptyState) elements.notesEmptyState.style.display = 'none';

    // If search active but no matching notes
    if (filtered.length === 0 && q) {
        elements.notesGrid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1; padding: 40px 20px;">
                <p style="color: var(--text-secondary); font-size: 0.95rem;">No sticky notes match "<strong>${escapeHtml(q)}</strong>".</p>
                <button class="btn btn-secondary" style="margin-top: 10px;" onclick="clearNotesSearch()">Clear Search</button>
            </div>
        `;
        return;
    }

    elements.notesGrid.innerHTML = '';
    filtered.forEach(note => {
        const noteEl = createStickyNoteElement(note);
        elements.notesGrid.appendChild(noteEl);
    });
}

/**
 * Creates DOM Element for an individual sticky note
 */
function createStickyNoteElement(note) {
    const card = document.createElement('article');
    const color = note.color || 'purple';
    card.className = `sticky-note note-color-${escapeHtml(color)} ${note.isPinned ? 'is-pinned' : ''}`;
    card.id = `noteCard-${note.noteId}`;
    card.dataset.noteId = note.noteId;

    const formattedDate = formatNoteDate(note.updatedAt || note.createdAt);

    const colors = ['purple', 'yellow', 'pink', 'blue', 'green'];
    const colorSwatchesHtml = colors.map(c => `
        <button type="button" class="color-dot-btn dot-${c} ${color === c ? 'active' : ''}" 
                data-color="${c}" title="Change color to ${c}" aria-label="Change note color to ${c}"></button>
    `).join('');

    card.innerHTML = `
        <div class="note-top-bar">
            <button type="button" class="note-pin-btn ${note.isPinned ? 'is-pinned' : ''}" 
                    title="${note.isPinned ? 'Unpin note' : 'Pin to top'}" aria-label="${note.isPinned ? 'Unpin note' : 'Pin note'}">
                <span class="pin-icon">📌</span>
                <span class="pin-text">${note.isPinned ? 'Pinned' : 'Pin'}</span>
            </button>
            <span class="note-save-badge saved" id="saveBadge-${note.noteId}">Saved</span>
        </div>

        <div class="note-body">
            <input type="text" class="note-title-input" value="${escapeHtml(note.title || '')}" 
                   placeholder="Note title..." aria-label="Note title" maxlength="150">
            <textarea class="note-content-input" placeholder="Jot down thoughts, ideas, or reminders..." 
                      aria-label="Note content" rows="5">${escapeHtml(note.content || '')}</textarea>
        </div>

        <div class="note-footer">
            <div class="note-footer-left">
                <span class="note-date-text" id="noteDate-${note.noteId}" title="${escapeHtml(note.updatedAt || note.createdAt || '')}">
                    ${formattedDate ? 'Updated ' + formattedDate : 'Just now'}
                </span>
            </div>
            <div class="note-actions">
                <div class="note-color-picker" role="group" aria-label="Note color options">
                    ${colorSwatchesHtml}
                </div>
                <button type="button" class="note-delete-btn" title="Delete note" aria-label="Delete note">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>
            </div>
        </div>
    `;

    // DOM References inside card
    const titleInput = card.querySelector('.note-title-input');
    const contentInput = card.querySelector('.note-content-input');
    const pinBtn = card.querySelector('.note-pin-btn');
    const deleteBtn = card.querySelector('.note-delete-btn');
    const colorDots = card.querySelectorAll('.color-dot-btn');

    // Auto-save on typing (debounced)
    const handleInput = () => {
        queueNoteAutoSave(note.noteId, titleInput.value, contentInput.value);
    };

    titleInput.addEventListener('input', handleInput);
    contentInput.addEventListener('input', handleInput);

    // Pin toggle
    pinBtn.addEventListener('click', () => {
        toggleNotePin(note.noteId, !note.isPinned);
    });

    // Delete note
    deleteBtn.addEventListener('click', () => {
        promptDeleteNote(note.noteId, titleInput.value.trim());
    });

    // Color switcher
    colorDots.forEach(dot => {
        dot.addEventListener('click', () => {
            const newColor = dot.getAttribute('data-color');
            if (newColor && newColor !== note.color) {
                changeNoteColor(note.noteId, newColor);
            }
        });
    });

    // Touch spring-bounce interaction for mobile/tablet tap
    let touchStartX = 0;
    let touchStartY = 0;
    card.addEventListener('touchstart', (e) => {
        if (e.touches && e.touches[0]) {
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
        }
    }, { passive: true });

    card.addEventListener('touchend', (e) => {
        if (e.changedTouches && e.changedTouches[0]) {
            const dx = Math.abs(e.changedTouches[0].clientX - touchStartX);
            const dy = Math.abs(e.changedTouches[0].clientY - touchStartY);
            // Only trigger bounce on intentional tap (not while scrolling)
            if (dx < 12 && dy < 12) {
                card.classList.remove('touch-bounced');
                void card.offsetWidth; // Trigger reflow
                card.classList.add('touch-bounced');
                setTimeout(() => {
                    card.classList.remove('touch-bounced');
                }, 600);
            }
        }
    }, { passive: true });

    return card;
}

/**
 * Debounced Auto-Save for Sticky Notes
 */
function queueNoteAutoSave(noteId, title, content) {
    const saveBadge = document.getElementById(`saveBadge-${noteId}`);
    if (saveBadge) {
        saveBadge.textContent = 'Saving...';
        saveBadge.className = 'note-save-badge saving';
    }

    // Clear previous debounce timer for this note
    if (state.noteSaveTimers[noteId]) {
        clearTimeout(state.noteSaveTimers[noteId]);
    }

    state.noteSaveTimers[noteId] = setTimeout(async () => {
        try {
            const params = new URLSearchParams();
            params.append('action', 'update');
            params.append('noteId', noteId);
            params.append('title', title);
            params.append('content', content);

            const res = await fetch(API.NOTES, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: params.toString()
            });

            const data = await res.json();
            if (data.success && data.note) {
                // Update local note object
                const idx = state.notes.findIndex(n => n.noteId === noteId);
                if (idx !== -1) {
                    state.notes[idx].title = data.note.title;
                    state.notes[idx].content = data.note.content;
                    state.notes[idx].updatedAt = data.note.updatedAt;
                }
                const dateEl = document.getElementById(`noteDate-${noteId}`);
                if (dateEl && data.note.updatedAt) {
                    dateEl.textContent = 'Updated ' + formatNoteDate(data.note.updatedAt);
                }
                if (saveBadge) {
                    saveBadge.textContent = 'Saved';
                    saveBadge.className = 'note-save-badge saved';
                }
            } else {
                if (saveBadge) {
                    saveBadge.textContent = 'Error';
                    saveBadge.className = 'note-save-badge';
                    saveBadge.style.color = '#ef4444';
                }
                showToast(data.message || 'Auto-save failed', 'error');
            }
        } catch (err) {
            console.error('Auto-save error:', err);
            if (saveBadge) {
                saveBadge.textContent = 'Offline';
                saveBadge.className = 'note-save-badge';
                saveBadge.style.color = '#ef4444';
            }
        }
    }, 600);
}

/**
 * Toggles Note Pin status
 */
async function toggleNotePin(noteId, newIsPinned) {
    try {
        const params = new URLSearchParams();
        params.append('action', 'pin');
        params.append('noteId', noteId);
        params.append('isPinned', newIsPinned ? 'true' : 'false');

        const res = await fetch(API.NOTES, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await res.json();
        if (data.success) {
            showToast(newIsPinned ? '📌 Note pinned to top' : 'Note unpinned', 'info');
            loadNotes(false);
        } else {
            showToast(data.message || 'Failed to update pin', 'error');
        }
    } catch (err) {
        console.error('Pin note error:', err);
        showToast('Error updating pin status', 'error');
    }
}

/**
 * Changes Note Color
 */
async function changeNoteColor(noteId, newColor) {
    try {
        const params = new URLSearchParams();
        params.append('action', 'color');
        params.append('noteId', noteId);
        params.append('color', newColor);

        const res = await fetch(API.NOTES, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await res.json();
        if (data.success) {
            const card = document.getElementById(`noteCard-${noteId}`);
            if (card) {
                // Update card theme class
                ['yellow', 'pink', 'blue', 'green', 'purple'].forEach(c => {
                    card.classList.remove(`note-color-${c}`);
                });
                card.classList.add(`note-color-${newColor}`);

                // Update active dot in swatch
                card.querySelectorAll('.color-dot-btn').forEach(dot => {
                    dot.classList.toggle('active', dot.getAttribute('data-color') === newColor);
                });
            }
            const idx = state.notes.findIndex(n => n.noteId === noteId);
            if (idx !== -1) {
                state.notes[idx].color = newColor;
            }
        } else {
            showToast(data.message || 'Failed to change color', 'error');
        }
    } catch (err) {
        console.error('Color change error:', err);
        showToast('Error changing note color', 'error');
    }
}

/**
 * Creates a brand new Sticky Note
 */
async function createNewNote() {
    try {
        const params = new URLSearchParams();
        params.append('action', 'create');
        params.append('title', '');
        params.append('content', '');
        params.append('color', 'purple');

        const res = await fetch(API.NOTES, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await res.json();
        if (data.success && data.note) {
            showToast('New sticky note created! Start typing...', 'success');
            // Insert at the front
            state.notes.unshift(data.note);
            // Re-sort: pinned first, then updated_at desc / noteId desc
            state.notes.sort((a, b) => {
                if (a.isPinned !== b.isPinned) return b.isPinned ? 1 : -1;
                return (b.noteId || 0) - (a.noteId || 0);
            });
            renderNotes();

            // Focus title input of new note
            setTimeout(() => {
                const newCard = document.getElementById(`noteCard-${data.note.noteId}`);
                if (newCard) {
                    const titleInput = newCard.querySelector('.note-title-input');
                    if (titleInput) {
                        titleInput.focus();
                    }
                    newCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }, 80);
        } else {
            showToast(data.message || 'Failed to create note', 'error');
        }
    } catch (err) {
        console.error('Create note error:', err);
        showToast('Error creating new sticky note', 'error');
    }
}

/**
 * Prompts user to confirm deleting a Sticky Note
 */
function promptDeleteNote(noteId, title) {
    state.noteToDeleteId = noteId;
    state.taskToDeleteId = null;

    if (elements.deleteModalTitle) elements.deleteModalTitle.textContent = 'Delete Sticky Note?';
    if (elements.deleteModalDesc) {
        const displayName = title ? `"${title}"` : 'this sticky note';
        elements.deleteModalDesc.innerHTML = `Are you sure you want to permanently delete <strong>${escapeHtml(displayName)}</strong>? This action cannot be undone.`;
    }
    if (elements.confirmDeleteBtnText) elements.confirmDeleteBtnText.textContent = 'Delete Note';

    if (elements.deleteModal) {
        elements.deleteModal.style.display = 'flex';
    }
}

/**
 * Executes deletion of the selected Sticky Note
 */
async function executeDeleteNote() {
    if (!state.noteToDeleteId) return;
    const noteId = state.noteToDeleteId;

    try {
        const params = new URLSearchParams();
        params.append('action', 'delete');
        params.append('noteId', noteId);

        const res = await fetch(API.NOTES, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: params.toString()
        });

        const data = await res.json();
        if (data.success) {
            showToast('Sticky note deleted from database.', 'success');
            // Remove from state
            state.notes = state.notes.filter(n => n.noteId !== noteId);
            closeDeleteModal();

            // Animate card removal if present
            const card = document.getElementById(`noteCard-${noteId}`);
            if (card) {
                card.style.transform = 'scale(0.8)';
                card.style.opacity = '0';
                card.style.transition = 'all 0.25s ease';
                setTimeout(() => renderNotes(), 250);
            } else {
                renderNotes();
            }
        } else {
            showToast(data.message || 'Failed to delete note', 'error');
        }
    } catch (err) {
        console.error('Delete note error:', err);
        showToast('Error communicating with server', 'error');
    }
}

/**
 * Formats note timestamp for display
 */
function formatNoteDate(dateStr) {
    if (!dateStr) return '';
    try {
        const date = new Date(dateStr.replace(' ', 'T'));
        if (isNaN(date.getTime())) return dateStr.split(' ')[0] || '';
        return date.toLocaleDateString(undefined, {
            month: 'short',
            day: 'numeric'
        });
    } catch {
        return '';
    }
}

/**
 * =====================================================================
 * Monthly Productivity Tracker Module
 * =====================================================================
 */

/**
 * Loads monthly productivity metrics from backend for state.productivityYear & state.productivityMonth
 */
async function loadProductivity(showToastNotice = false) {
    if (!elements.viewProductivity) return;

    if (elements.prodLoadingIndicator) {
        elements.prodLoadingIndicator.style.display = 'flex';
    }
    if (elements.prodChartWrapper) {
        elements.prodChartWrapper.style.opacity = '0.5';
    }

    try {
        const url = `${API.PRODUCTIVITY}?year=${state.productivityYear}&month=${state.productivityMonth}`;
        const response = await fetch(url);
        const data = await response.json();

        if (data.success && data.productivity) {
            state.productivityData = data.productivity;
            renderProductivity();
            if (showToastNotice) {
                showToast(`Productivity updated for ${data.productivity.monthName}`, 'info');
            }
        } else {
            showToast(data.message || 'Failed to load productivity metrics', 'error');
        }
    } catch (err) {
        console.error('Error fetching productivity metrics:', err);
        showToast('Unable to reach server to fetch productivity tracker.', 'error');
    } finally {
        if (elements.prodLoadingIndicator) {
            elements.prodLoadingIndicator.style.display = 'none';
        }
        if (elements.prodChartWrapper) {
            elements.prodChartWrapper.style.opacity = '1';
        }
    }
}

/**
 * Renders Overview Cards, Daily Chart, Peak Day & Comparison from state.productivityData
 */
function renderProductivity() {
    const prod = state.productivityData;
    if (!prod) return;

    // 1. Month Header
    if (elements.prodMonthTitle) {
        elements.prodMonthTitle.textContent = prod.monthName;
    }
    if (elements.prodMonthBadge) {
        elements.prodMonthBadge.textContent = `${prod.daysInMonth} Days`;
    }

    // 2. Overview Metric Cards
    if (elements.prodStatCompleted) {
        const currentVal = parseInt(elements.prodStatCompleted.textContent) || 0;
        animateValue(elements.prodStatCompleted, currentVal, prod.tasksCompleted, 300);
    }
    if (elements.prodStatCompletedSubtext) {
        elements.prodStatCompletedSubtext.textContent = `Completed in ${prod.monthName}`;
    }

    if (elements.prodStatCreated) {
        const currentVal = parseInt(elements.prodStatCreated.textContent) || 0;
        animateValue(elements.prodStatCreated, currentVal, prod.tasksCreated, 300);
    }
    if (elements.prodStatCreatedSubtext) {
        elements.prodStatCreatedSubtext.textContent = `Created in ${prod.monthName}`;
    }

    if (elements.prodStatDue) {
        const currentVal = parseInt(elements.prodStatDue.textContent) || 0;
        animateValue(elements.prodStatDue, currentVal, prod.tasksDue, 300);
    }
    if (elements.prodStatDueSubtext) {
        elements.prodStatDueSubtext.textContent = `Deadlines in ${prod.monthName}`;
    }

    if (elements.prodStatRate) {
        const currentVal = parseInt(elements.prodStatRate.textContent) || 0;
        animateValue(elements.prodStatRate, currentVal, Math.round(prod.completionRate), 350, '%');
    }
    if (elements.prodStatRateSubtext) {
        elements.prodStatRateSubtext.textContent = prod.tasksCreated > 0 
            ? `${prod.tasksCompleted} completed / ${prod.tasksCreated} created`
            : '0 tasks created this month';
    }

    // 3. Daily Completion Chart
    renderProductivityChart(prod);

    // 4. Most Productive Day Card
    if (elements.prodPeakDayText) {
        elements.prodPeakDayText.textContent = prod.mostProductiveSummary;
    }
    if (elements.prodPeakDayDesc) {
        if (prod.mostProductiveCount > 0) {
            elements.prodPeakDayDesc.textContent = `Peak task completion volume achieved in ${prod.monthName}.`;
        } else {
            elements.prodPeakDayDesc.textContent = `No tasks were marked as completed in ${prod.monthName}.`;
        }
    }

    // 5. Monthly Comparison Card
    if (elements.prodComparisonText) {
        elements.prodComparisonText.textContent = prod.comparisonMessage;
    }
    if (elements.prodComparisonDesc) {
        elements.prodComparisonDesc.textContent = `Completed tasks: ${prod.tasksCompleted} in ${prod.monthName} vs ${prod.previousMonthCompleted} in previous month.`;
    }
    if (elements.prodTrendIconBox) {
        if (prod.completedDifference > 0) {
            elements.prodTrendIconBox.textContent = '📈';
            elements.prodTrendIconBox.style.color = '#10b981';
        } else if (prod.completedDifference < 0) {
            elements.prodTrendIconBox.textContent = '📉';
            elements.prodTrendIconBox.style.color = '#ef4444';
        } else {
            elements.prodTrendIconBox.textContent = '📊';
            elements.prodTrendIconBox.style.color = '#C4B5FD';
        }
    }
}

/**
 * Renders the daily bars and horizontal/vertical labels in the productivity chart
 */
function renderProductivityChart(prod) {
    if (!elements.prodChartBars) return;

    if (prod.tasksCompleted === 0) {
        if (elements.prodChartEmpty) {
            elements.prodChartEmpty.style.display = 'flex';
        }
        if (elements.prodChartWrapper) {
            elements.prodChartWrapper.style.display = 'none';
        }
        if (elements.prodEmptyMonthName) {
            elements.prodEmptyMonthName.textContent = prod.monthName;
        }
        return;
    }

    if (elements.prodChartEmpty) {
        elements.prodChartEmpty.style.display = 'none';
    }
    if (elements.prodChartWrapper) {
        elements.prodChartWrapper.style.display = 'block';
    }

    const maxCount = Math.max(1, prod.mostProductiveCount);
    if (elements.prodScaleHint) {
        elements.prodScaleHint.textContent = `Peak: ${maxCount} task${maxCount > 1 ? 's' : ''} | Horizontal: Days 1 to ${prod.daysInMonth}`;
    }

    elements.prodChartBars.innerHTML = '';

    prod.dailyCompletions.forEach(item => {
        const col = document.createElement('div');
        col.className = 'chart-day-col';

        const isPeak = (item.count > 0 && item.count === maxCount);
        const hasCount = (item.count > 0);
        // Height proportional to max count (up to 85% of available height)
        const heightPercent = hasCount ? Math.max(8, Math.round((item.count / maxCount) * 85)) : 0;

        const countBadgeHtml = hasCount
            ? `<span class="chart-count-label">${item.count}</span>`
            : '';

        const barClass = hasCount 
            ? (isPeak ? 'chart-bar-fill is-peak' : 'chart-bar-fill has-count')
            : 'chart-bar-fill zero-count';

        const dateParts = item.date.split('-');
        const readableDate = dateParts.length === 3 
            ? new Date(dateParts[0], dateParts[1] - 1, dateParts[2]).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
            : item.date;

        col.innerHTML = `
            <div class="chart-tooltip">${escapeHtml(readableDate)}: ${item.count} task${item.count === 1 ? '' : 's'} completed</div>
            <div class="chart-bar-container">
                ${countBadgeHtml}
                <div class="${barClass}" style="height: ${hasCount ? heightPercent + '%' : '3px'};"></div>
            </div>
            <span class="chart-day-number">${item.day}</span>
        `;

        elements.prodChartBars.appendChild(col);
    });
}



// ==========================================
// Authentication & Session Management System
// ==========================================
function initAuthEventListeners() {
    const userPill = document.getElementById('userPill');
    const logoutBtn = document.getElementById('logoutBtn');
    const authModalOverlay = document.getElementById('authModalOverlay');
    const tabSignInBtn = document.getElementById('tabSignInBtn');
    const tabRegisterBtn = document.getElementById('tabRegisterBtn');
    const signInForm = document.getElementById('signInForm');
    const registerForm = document.getElementById('registerForm');

    if (userPill) {
        userPill.addEventListener('click', (e) => {
            if (e.target.closest('#logoutBtn')) return;
            if (!state.currentUser) {
                showAuthModal('login');
            }
        });
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            await performLogout();
        });
    }

    if (tabSignInBtn && tabRegisterBtn) {
        tabSignInBtn.addEventListener('click', () => switchAuthTab('login'));
        tabRegisterBtn.addEventListener('click', () => switchAuthTab('register'));
    }

    if (signInForm) {
        signInForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await performLogin();
        });
    }

    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await performRegister();
        });
    }
}

function switchAuthTab(tab) {
    const tabSignInBtn = document.getElementById('tabSignInBtn');
    const tabRegisterBtn = document.getElementById('tabRegisterBtn');
    const signInForm = document.getElementById('signInForm');
    const registerForm = document.getElementById('registerForm');
    const authModalTitle = document.getElementById('authModalTitle');
    const authErrorAlert = document.getElementById('authErrorAlert');

    if (authErrorAlert) authErrorAlert.style.display = 'none';

    if (tab === 'login') {
        tabSignInBtn.className = 'btn btn-primary';
        tabRegisterBtn.className = 'btn btn-secondary';
        signInForm.style.display = 'block';
        registerForm.style.display = 'none';
        if (authModalTitle) authModalTitle.textContent = 'Sign In to TaskVerse';
    } else {
        tabSignInBtn.className = 'btn btn-secondary';
        tabRegisterBtn.className = 'btn btn-primary';
        signInForm.style.display = 'none';
        registerForm.style.display = 'block';
        if (authModalTitle) authModalTitle.textContent = 'Create TaskVerse Account';
    }
}

function showAuthModal(tab = 'login') {
    const authModalOverlay = document.getElementById('authModalOverlay');
    if (authModalOverlay) {
        switchAuthTab(tab);
        authModalOverlay.style.display = 'flex';
    }
}

function hideAuthModal() {
    const authModalOverlay = document.getElementById('authModalOverlay');
    if (authModalOverlay) {
        authModalOverlay.style.display = 'none';
    }
}

function showAuthError(msg) {
    const alertBox = document.getElementById('authErrorAlert');
    const alertText = document.getElementById('authErrorText');
    if (alertBox && alertText) {
        alertText.textContent = msg;
        alertBox.style.display = 'block';
    }
}

function updateUserPillUI(user) {
    const nameDisplay = document.getElementById('userNameDisplay');
    const roleDisplay = document.getElementById('userRoleDisplay');
    const avatar = document.getElementById('userAvatar');
    const logoutBtn = document.getElementById('logoutBtn');

    if (user) {
        if (nameDisplay) nameDisplay.textContent = user.username;
        if (roleDisplay) roleDisplay.textContent = user.role === 'OWNER' ? 'Workspace Owner' : 'Team Member';
        if (avatar) avatar.textContent = user.username.substring(0, 2).toUpperCase();
        if (logoutBtn) logoutBtn.style.display = 'block';
    } else {
        if (nameDisplay) nameDisplay.textContent = 'Guest User';
        if (roleDisplay) roleDisplay.textContent = 'Click to Sign In';
        if (avatar) avatar.textContent = 'TV';
        if (logoutBtn) logoutBtn.style.display = 'none';
    }
}

async function checkAuthAndInit() {
    try {
        const res = await fetch(API.AUTH + '/me');
        const data = await res.json();
        if (data.authenticated && data.user) {
            state.currentUser = data.user;
            state.csrfToken = data.csrfToken;
            updateUserPillUI(data.user);
            hideAuthModal();
            checkHealth();
            loadDashboard();
            const initialView = getRouteFromHash();
            navigateTo(initialView, false);
        } else {
            state.currentUser = null;
            state.csrfToken = data.csrfToken || null;
            updateUserPillUI(null);
            showAuthModal('login');
            checkHealth();
        }
    } catch (e) {
        state.currentUser = null;
        updateUserPillUI(null);
        showAuthModal('login');
        checkHealth();
    }
}

async function performLogin() {
    const identifier = document.getElementById('authLoginIdentifier').value.trim();
    const password = document.getElementById('authLoginPassword').value;
    const submitBtn = document.getElementById('submitLoginBtn');
    const submitText = document.getElementById('submitLoginText');

    if (!identifier || !password) {
        showAuthError('Please enter both identifier and password.');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitText.textContent = 'Signing in...';

        const params = new URLSearchParams();
        params.append('identifier', identifier);
        params.append('password', password);

        const res = await fetch(API.AUTH + '/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        });

        const data = await res.json();
        if (res.ok && data.success) {
            state.currentUser = data.user;
            state.csrfToken = data.csrfToken;
            updateUserPillUI(data.user);
            hideAuthModal();
            loadDashboard();
            showCelebrationToast('Welcome back, ' + data.user.username + '!');
        } else {
            showAuthError(data.message || 'Invalid credentials.');
        }
    } catch (e) {
        showAuthError('Unable to connect to authentication service.');
    } finally {
        submitBtn.disabled = false;
        submitText.textContent = 'Sign In';
    }
}

async function performRegister() {
    const username = document.getElementById('authRegisterUsername').value.trim();
    const email = document.getElementById('authRegisterEmail').value.trim();
    const password = document.getElementById('authRegisterPassword').value;
    const submitBtn = document.getElementById('submitRegisterBtn');
    const submitText = document.getElementById('submitRegisterText');

    if (!username || !email || !password) {
        showAuthError('All fields are required.');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitText.textContent = 'Creating account...';

        const params = new URLSearchParams();
        params.append('username', username);
        params.append('email', email);
        params.append('password', password);

        const res = await fetch(API.AUTH + '/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        });

        const data = await res.json();
        if (res.ok && data.success) {
            state.currentUser = data.user;
            state.csrfToken = data.csrfToken;
            updateUserPillUI(data.user);
            hideAuthModal();
            loadDashboard();
            showCelebrationToast('Account created! Welcome, ' + data.user.username + '!');
        } else {
            showAuthError(data.message || 'Registration failed.');
        }
    } catch (e) {
        showAuthError('Unable to connect to registration service.');
    } finally {
        submitBtn.disabled = false;
        submitText.textContent = 'Create Account';
    }
}

async function performLogout() {
    try {
        await fetch(API.AUTH + '/logout', {
            method: 'POST',
            headers: getAuthHeaders()
        });
    } catch (ignored) {}

    state.currentUser = null;
    state.csrfToken = null;
    state.tasks = [];
    state.notes = [];
    state.totalPoints = 0;
    updateUserPillUI(null);
    showAuthModal('login');
}
