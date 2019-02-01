package auskov;

import java.io.IOException;

public class MyCacheSimpleImplTest extends AbstractMyCacheTest {
    @Override
    MyCache createANewCache() throws IOException {
        return MyCacheSimpleImpl.createCash();
    }
}