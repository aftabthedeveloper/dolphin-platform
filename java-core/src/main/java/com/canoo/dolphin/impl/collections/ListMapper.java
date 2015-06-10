package com.canoo.dolphin.impl.collections;

import com.canoo.dolphin.collections.ListChangeEvent;
import com.canoo.dolphin.impl.BeanRepository;
import com.canoo.dolphin.impl.ClassRepository;
import com.canoo.dolphin.impl.PresentationModelBuilderFactory;
import com.canoo.dolphin.impl.info.ClassInfo;
import com.canoo.dolphin.impl.info.PropertyInfo;
import org.opendolphin.core.Dolphin;
import org.opendolphin.core.ModelStoreEvent;
import org.opendolphin.core.ModelStoreListener;
import org.opendolphin.core.PresentationModel;

import java.util.List;

public abstract class ListMapper {

    private final Dolphin dolphin;
    private final BeanRepository beanRepository;
    private final ClassRepository classRepository;
    protected final PresentationModelBuilderFactory builderFactory;

    public ListMapper(Dolphin dolphin, ClassRepository classRepository, BeanRepository beanRepository, PresentationModelBuilderFactory builderFactory) {
        this.dolphin = dolphin;
        this.beanRepository = beanRepository;
        this.classRepository = classRepository;
        this.builderFactory = builderFactory;

        dolphin.addModelStoreListener(getAddEntryKey(), createAddListener());
        dolphin.addModelStoreListener(getDelEntryKey(), createDeleteListener());
        dolphin.addModelStoreListener(getSetEntryKey(), createSetListener());
    }

    protected abstract String getAddEntryKey();
    protected abstract String getDelEntryKey();
    protected abstract String getSetEntryKey();

    private ModelStoreListener createAddListener() {
        return new ModelStoreListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void modelStoreChanged(ModelStoreEvent modelStoreEvent) {
                if (modelStoreEvent.getType() == ModelStoreEvent.Type.ADDED) {
                    PresentationModel model = null;
                    try {
                        model = modelStoreEvent.getPresentationModel();
                        final String sourceId = model.findAttributeByPropertyName("source").getValue().toString();
                        final String attributeName = model.findAttributeByPropertyName("attribute").getValue().toString();

                        final Object bean = beanRepository.getBean(sourceId);
                        final ClassInfo classInfo = classRepository.getOrCreateClassInfo(bean.getClass());
                        final PropertyInfo observableListInfo = classInfo.getObservableListInfo(attributeName);

                        final ObservableArrayList list = (ObservableArrayList) observableListInfo.getPrivileged(bean);

                        final int pos = (Integer) model.findAttributeByPropertyName("pos").getValue();
                        final Object dolphinValue = model.findAttributeByPropertyName("element").getValue();

                        final Object value = observableListInfo.convertFromDolphin(dolphinValue);
                        list.internalAdd(pos, value);
                    } catch (NullPointerException | ClassCastException ex) {
                        System.out.println("Invalid ADD_FROM_CLIENT command received: " + model);
                    } finally {
                        if (model != null) {
                            dolphin.remove(model);
                        }
                    }
                }
            }
        };
    }

    private ModelStoreListener createDeleteListener() {
        return new ModelStoreListener() {
            @Override
            public void modelStoreChanged(ModelStoreEvent modelStoreEvent) {
                if (modelStoreEvent.getType() == ModelStoreEvent.Type.ADDED) {
                    PresentationModel model = null;
                    try {
                        model = modelStoreEvent.getPresentationModel();
                        final String sourceId = model.findAttributeByPropertyName("source").getValue().toString();
                        final String attributeName = model.findAttributeByPropertyName("attribute").getValue().toString();

                        final Object bean = beanRepository.getBean(sourceId);
                        final ClassInfo classInfo = classRepository.getOrCreateClassInfo(bean.getClass());
                        final PropertyInfo observableListInfo = classInfo.getObservableListInfo(attributeName);

                        final ObservableArrayList list = (ObservableArrayList) observableListInfo.getPrivileged(bean);

                        int from = (Integer) model.findAttributeByPropertyName("from").getValue();
                        int to = (Integer) model.findAttributeByPropertyName("to").getValue();
                        list.internalDelete(from, to);
                    } catch (NullPointerException | ClassCastException ex) {
                        System.out.println("Invalid ADD_FROM_CLIENT command received: " + model);
                    } finally {
                        if (model != null) {
                            dolphin.remove(model);
                        }
                    }
                }
            }
        };
    }

    private ModelStoreListener createSetListener() {
        return new ModelStoreListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void modelStoreChanged(ModelStoreEvent modelStoreEvent) {
                if (modelStoreEvent.getType() == ModelStoreEvent.Type.ADDED) {
                    PresentationModel model = null;
                    try {
                        model = modelStoreEvent.getPresentationModel();
                        final String sourceId = model.findAttributeByPropertyName("source").getValue().toString();
                        final String attributeName = model.findAttributeByPropertyName("attribute").getValue().toString();

                        final Object bean = beanRepository.getBean(sourceId);
                        final ClassInfo classInfo = classRepository.getOrCreateClassInfo(bean.getClass());
                        final PropertyInfo observableListInfo = classInfo.getObservableListInfo(attributeName);

                        final ObservableArrayList list = (ObservableArrayList) observableListInfo.getPrivileged(bean);

                        final int pos = (Integer) model.findAttributeByPropertyName("pos").getValue();
                        final Object dolphinValue = model.findAttributeByPropertyName("element").getValue();
                        final Object value = observableListInfo.convertFromDolphin(dolphinValue);
                        list.internalReplace(pos, value);
                    } catch (NullPointerException | ClassCastException ex) {
                        System.out.println("Invalid ADD_FROM_CLIENT command received: " + model);
                    } finally {
                        if (model != null) {
                            dolphin.remove(model);
                        }
                    }
                }
            }
        };
    }

    public void processEvent(PropertyInfo observableListInfo, String sourceId, ListChangeEvent<?> event) {
        final String attributeName = observableListInfo.getAttributeName();

        for (final ListChangeEvent.Change<?> change : event.getChanges()) {

            final int to = change.getTo();
            int from = change.getFrom();
            int removedCount = change.getRemovedElements().size();

            if (change.isReplaced()) {
                final int n = Math.min(to - from, removedCount);
                final List<?> newElements = event.getSource().subList(from, from + n);
                int pos = from;
                for (final Object element : newElements) {
                    final Object value = observableListInfo.convertToDolphin(element);
                    sendReplace(sourceId, attributeName, pos++, value);
                }
                from += n;
                removedCount -= n;
            }
            if (to > from) {
                final List<?> newElements = event.getSource().subList(from, to);
                int pos = from;
                for (final Object element : newElements) {
                    final Object value = observableListInfo.convertToDolphin(element);
                    sendAdd(sourceId, attributeName, pos++, value);
                }
            } else if (removedCount > 0) {
                sendRemove(sourceId, attributeName, from, from + removedCount);
            }
        }
    }


    protected abstract void sendAdd(String sourceId, String attributeName, int pos, Object element);

    protected abstract void sendRemove(String sourceId, String attributeName, int from, int to);

    protected abstract void sendReplace(String sourceId, String attributeName, int pos, Object element);
}
