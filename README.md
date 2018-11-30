# ModuleResRename
组件化工具，将res下的所有文件、layout中的id，string/color/dimens等的值，增加统一前缀

rename all res filename with a uniform prefix(dimen/color/layout/drawable etc.)
 aaa.png -> prefix_aaa.png
 test_layout.xml -> prefix_test_layout.xml
 
rename all ids dimens colors strings name with prefix
  android:id="@+id/text"  ->  android:id="@+id/prefix_text"
