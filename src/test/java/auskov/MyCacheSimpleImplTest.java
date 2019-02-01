package auskov;

import java.io.IOException;

public class MyCacheSimpleImplTest extends AbstractMyCacheTest {
    @Override
    protected MyCache createANewCache() throws IOException {
        return MyCacheSimpleImpl.createCash();
    }
}