package teefourteen.glideplayer;

import android.support.annotation.MenuRes;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * Creates an interface for fragments inside an activity to change the actionbar temporarily
 */

abstract public class CustomToolbarOptions {
    private String currentMenu;
    private AppCompatActivity activity;
    private ArrayList<Integer> menus;
    private ArrayList<String> menuNames;

    /**To be implemented by fragment*/
    public interface MenuHandler {
        /**Used to make any initial setup to a toolbar other than menu inflation (such as removing
         * title or drawer toggle)*/
        public void setupToolbar(AppCompatActivity activity);
        /**Implemented to receive id of the pressed option. Don't call directly*/
        public void handleOption(int optionItemId);
    }
    ArrayList<MenuHandler> menuHandlers;

    public CustomToolbarOptions(AppCompatActivity activity) {
        menus = new ArrayList<>();
        menuNames = new ArrayList<>();
        menuHandlers = new ArrayList<>();
        currentMenu = null;
        this.activity = activity;
    }
    /**Should be called inside activity when toolbar/action options are to be inflated*/
    public @MenuRes int getCurrentMenu(){
        if(currentMenu == null) return -1;
        else return menus.get(menuNames.indexOf(currentMenu));
    }
    /**Register a new menu for the actionbar and specify handlers*/
    public boolean registerMenu(@MenuRes int menuRes, String menuName, MenuHandler menuHandler) {
        if(menuNames.contains(menuName))
            return false;
        else {
            menus.add(menuRes);
            menuNames.add(menuName);
            menuHandlers.add(menuHandler);
            return true;
        }
    }

    public boolean unregisterMenu(String menuName){
        if(menuNames.contains(menuName)){
            menuHandlers.remove(menuNames.indexOf(menuName));
            menus.remove(menuNames.indexOf(menuName));
            menuNames.remove(menuName);
            return true;
        } else return false;
    }

    /**When one fragment wants to change to toolbar so that it can use its custom menu, call this.
     * Invokes setup method implemented by fragment
     */
    public boolean changeToolbar(String menuName) {
        if(menuNames.contains(menuName)) {
            currentMenu = menuName;
            int index = menuNames.indexOf(menuName);
            reInflateMenu();
            menuHandlers.get(index).setupToolbar(activity);
            return true;
        }
        else
            return false;
    }
    /**Call this from fragment to return toolbar to how it was before. Calls resetToolbar*/
    public void returnToNormal() {
        currentMenu = null;
        resetToolbar();
        reInflateMenu();

    }
    /**Call this from activity when actionbar option is tapped*/
    public void handleOption(int optionItemId) {
        if(currentMenu!=null)
            menuHandlers.get(menuNames.indexOf(currentMenu)).handleOption(optionItemId);
    }
    /**Should be implemented by activity. Can be used to call invalidateOptionsMenu for example*/
    abstract public void reInflateMenu();
    /**Should be implemented by activity. Use it to reset toolbar to normal in case a fragment
     * changed it.*/
    abstract public void resetToolbar();
}
