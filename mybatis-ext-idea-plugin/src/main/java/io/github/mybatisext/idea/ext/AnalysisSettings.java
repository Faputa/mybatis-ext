package io.github.mybatisext.idea.ext;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@Service(Service.Level.PROJECT)
@State(name = "MybatisExtAnalysis", storages = @Storage("mybatis-ext.xml"))
public final class AnalysisSettings implements PersistentStateComponent<AnalysisSettings.State> {

    public static final class State {

        public boolean defaultFilterable = true;
        /** 显式指定已在项目中配置 TypeHandler 的 Java 类型。 */
        public List<String> scalarTypes = new ArrayList<>();
    }

    private State state = new State();

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
}
