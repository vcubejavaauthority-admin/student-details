/**
 * Vcube Global Theme Manager
 * Handles Light/Dark mode transitions across all pages
 */
(function() {
    // Get theme from local storage or system preference
    const getPreferredTheme = () => {
        const storedTheme = localStorage.getItem('theme');
        if (storedTheme) {
            return storedTheme;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    };

    const setTheme = (theme) => {
        document.documentElement.setAttribute('data-theme', theme);
        const themeIcons = document.querySelectorAll('#themeIcon');
        themeIcons.forEach(icon => {
            if (icon) {
                icon.className = theme === 'dark' ? 'lucide-sun' : 'lucide-moon';
                if (typeof lucide !== 'undefined') lucide.createIcons(); // refresh icons
            }
        });
    };

    // Initialize theme immediately to avoid flash
    const currentTheme = getPreferredTheme();
    setTheme(currentTheme);

    // Initialize theme switchers when DOM is ready
    document.addEventListener('DOMContentLoaded', () => {
        const themeSwitchers = document.querySelectorAll('#themeSwitch');
        
        themeSwitchers.forEach(switcher => {
            switcher.addEventListener('click', () => {
                const currentTheme = document.documentElement.getAttribute('data-theme');
                const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
                localStorage.setItem('theme', newTheme);
                setTheme(newTheme);
            });
        });
        
        // Final check for icons after DOM load
        if (typeof lucide !== 'undefined') lucide.createIcons();
    });
})();
